package com.smartemergency.service;

import com.smartemergency.dto.request.EmergencyRequestDto;
import com.smartemergency.dto.request.StatusUpdateRequest;
import com.smartemergency.dto.response.EmergencyRequestResponse;
import com.smartemergency.entity.*;
import com.smartemergency.enums.AmbulanceStatus;
import com.smartemergency.enums.NotificationType;
import com.smartemergency.enums.RequestStatus;
import com.smartemergency.exception.BusinessException;
import com.smartemergency.exception.ResourceNotFoundException;
import com.smartemergency.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmergencyService {

    private final EmergencyRequestRepository emergencyRepo;
    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public EmergencyRequestResponse createRequest(Long patientId, EmergencyRequestDto dto) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
        Hospital hospital = null;
        if (dto.getHospitalId() != null) {
            hospital = hospitalRepository.findById(dto.getHospitalId())
                    .orElseThrow(() -> new ResourceNotFoundException("Hospital", dto.getHospitalId()));
        }
        String requestNumber = "ER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        EmergencyRequest request = EmergencyRequest.builder()
                .requestNumber(requestNumber).patient(patient).hospital(hospital)
                .requestType(dto.getRequestType()).priority(dto.getPriority())
                .status(RequestStatus.PENDING).patientCondition(dto.getPatientCondition())
                .symptoms(dto.getSymptoms()).patientLatitude(dto.getPatientLatitude())
                .patientLongitude(dto.getPatientLongitude()).patientAddress(dto.getPatientAddress())
                .notes(dto.getNotes()).isEmergency(Boolean.TRUE.equals(dto.getIsEmergency()))
                .build();
        EmergencyRequest saved = emergencyRepo.save(request);
        log.info("Emergency request created: {} for patient: {}", requestNumber, patient.getEmail());
        pushNotification(patient, "Request Submitted",
                "Request #" + requestNumber + " submitted successfully.", NotificationType.SUCCESS, saved.getId());
        if (hospital != null) {
            pushNotification(hospital.getUser(), "🚨 New Emergency Request",
                    "New " + dto.getPriority() + " request from " + patient.getFullName(),
                    NotificationType.EMERGENCY, saved.getId());
            messagingTemplate.convertAndSend("/topic/hospital/" + hospital.getId() + "/emergency", toResponse(saved));
        }
        messagingTemplate.convertAndSend("/topic/emergency/updates", toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public EmergencyRequestResponse updateRequestStatus(Long requestId, Long hospitalUserId,
                                                         StatusUpdateRequest statusReq) {
        EmergencyRequest request = emergencyRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency Request", requestId));
        User hospitalUser = userRepository.findById(hospitalUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", hospitalUserId));
        Hospital hospital = hospitalRepository.findByUser(hospitalUser)
                .orElseThrow(() -> new BusinessException("Hospital profile not found"));
        if (request.getHospital() != null && !request.getHospital().getId().equals(hospital.getId())) {
            throw new BusinessException("This request does not belong to your hospital");
        }
        request.setStatus(statusReq.getStatus());
        if (statusReq.getNotes() != null) request.setNotes(statusReq.getNotes());
        if (request.getHospital() == null && statusReq.getStatus() == RequestStatus.ACCEPTED) {
            request.setHospital(hospital);
        }
        if (statusReq.getAmbulanceId() != null && statusReq.getStatus() == RequestStatus.DISPATCHED) {
            Ambulance ambulance = ambulanceRepository.findById(statusReq.getAmbulanceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ambulance", statusReq.getAmbulanceId()));
            ambulance.setStatus(AmbulanceStatus.DISPATCHED);
            ambulanceRepository.save(ambulance);
            request.setAmbulance(ambulance);
            request.setEstimatedArrivalMinutes(statusReq.getEstimatedArrivalMinutes());
        }
        if (statusReq.getStatus() == RequestStatus.REJECTED) {
            request.setRejectionReason(statusReq.getRejectionReason());
        }
        if (statusReq.getStatus() == RequestStatus.COMPLETED) {
            request.setCompletedAt(LocalDateTime.now());
            if (request.getAmbulance() != null) {
                request.getAmbulance().setStatus(AmbulanceStatus.AVAILABLE);
                ambulanceRepository.save(request.getAmbulance());
            }
        }
        EmergencyRequest updated = emergencyRepo.save(request);
        pushNotification(request.getPatient(), "Request Updated",
                buildStatusMessage(statusReq.getStatus(), updated), NotificationType.INFO, requestId);
        messagingTemplate.convertAndSend("/topic/emergency/updates", toResponse(updated));
        return toResponse(updated);
    }

    @Transactional
    public EmergencyRequestResponse cancelByPatient(Long requestId, Long patientId) {
        EmergencyRequest request = emergencyRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency Request", requestId));
        if (!request.getPatient().getId().equals(patientId)) {
            throw new BusinessException("Unauthorized to cancel this request");
        }
        if (request.getStatus() == RequestStatus.COMPLETED || request.getStatus() == RequestStatus.CANCELLED) {
            throw new BusinessException("Cannot cancel a " + request.getStatus() + " request");
        }
        request.setStatus(RequestStatus.CANCELLED);
        request.setNotes("Cancelled by patient");
        EmergencyRequest updated = emergencyRepo.save(request);
        messagingTemplate.convertAndSend("/topic/emergency/updates", toResponse(updated));
        return toResponse(updated);
    }
     @Transactional(readOnly = true)
    public List<EmergencyRequestResponse> getPatientRequests(Long patientId) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
        return emergencyRepo.findByPatientOrderByCreatedAtDesc(patient)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public List<EmergencyRequestResponse> getHospitalActiveRequests(Long hospitalUserId) {
        User user = userRepository.findById(hospitalUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", hospitalUserId));
        Hospital hospital = hospitalRepository.findByUser(user)
                .orElseThrow(() -> new BusinessException("Hospital profile not found"));
        return emergencyRepo.findActiveRequestsByHospital(hospital)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public List<EmergencyRequestResponse> getAllHospitalRequests(Long hospitalUserId) {
        User user = userRepository.findById(hospitalUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", hospitalUserId));
        Hospital hospital = hospitalRepository.findByUser(user)
                .orElseThrow(() -> new BusinessException("Hospital profile not found"));
        return emergencyRepo.findByHospitalOrderByCreatedAtDesc(hospital, Pageable.unpaged())
                .getContent().stream().map(this::toResponse).collect(Collectors.toList());
    }
     @Transactional(readOnly = true)
    public List<EmergencyRequestResponse> getAllRequests() {
        return emergencyRepo.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public EmergencyRequestResponse getRequestById(Long requestId) {
        return toResponse(emergencyRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency Request", requestId)));
    }

    @Async
    protected void pushNotification(User user, String title, String message,
                                      NotificationType type, Long requestId) {
        try {
            Notification notification = Notification.builder()
                    .user(user).title(title).message(message)
                    .type(type).relatedRequestId(requestId).isRead(false).build();
            notificationRepository.save(notification);
            messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/notifications",
                    java.util.Map.of("title", title, "message", message, "type", type));
        } catch (Exception e) {
            log.warn("Push notification failed: {}", e.getMessage());
        }
    }

    private String buildStatusMessage(RequestStatus status, EmergencyRequest request) {
        return switch (status) {
            case ACCEPTED -> "Request #" + request.getRequestNumber() + " accepted by "
                    + (request.getHospital() != null ? request.getHospital().getName() : "hospital") + ".";
            case DISPATCHED -> "Ambulance dispatched! ETA: " + request.getEstimatedArrivalMinutes() + " min.";
            case EN_ROUTE -> "Ambulance is on the way to your location.";
            case ARRIVED -> "Ambulance has arrived at your location!";
            case COMPLETED -> "Your request has been completed. Stay safe!";
            case REJECTED -> "Request rejected. Reason: " + request.getRejectionReason();
            default -> "Request status updated to: " + status;
        };
    }

public EmergencyRequestResponse toResponse(EmergencyRequest req) {
    String patientName = null;
    String patientPhone = null;
    try {
        patientName = req.getPatient() != null ? req.getPatient().getFullName() : null;
        patientPhone = req.getPatient() != null ? req.getPatient().getPhone() : null;
    } catch (Exception e) {
        patientName = "Unknown";
    }

    String hospitalName = null;
    String hospitalPhone = null;
    Long hospitalId = null;
    try {
        hospitalId = req.getHospital() != null ? req.getHospital().getId() : null;
        hospitalName = req.getHospital() != null ? req.getHospital().getName() : null;
        hospitalPhone = req.getHospital() != null ? req.getHospital().getPhone() : null;
    } catch (Exception e) {
        hospitalName = null;
    }

    String ambulanceNumber = null;
    Long ambulanceId = null;
    try {
        ambulanceId = req.getAmbulance() != null ? req.getAmbulance().getId() : null;
        ambulanceNumber = req.getAmbulance() != null ? req.getAmbulance().getVehicleNumber() : null;
    } catch (Exception e) {
        ambulanceNumber = null;
    }

    return EmergencyRequestResponse.builder()
            .id(req.getId())
            .requestNumber(req.getRequestNumber())
            .patientId(req.getPatient() != null ? req.getPatient().getId() : null)
            .patientName(patientName)
            .patientPhone(patientPhone)
            .hospitalId(hospitalId)
            .hospitalName(hospitalName)
            .hospitalPhone(hospitalPhone)
            .ambulanceId(ambulanceId)
            .ambulanceNumber(ambulanceNumber)
            .requestType(req.getRequestType())
            .priority(req.getPriority())
            .status(req.getStatus())
            .patientCondition(req.getPatientCondition())
            .symptoms(req.getSymptoms())
            .patientLatitude(req.getPatientLatitude())
            .patientLongitude(req.getPatientLongitude())
            .patientAddress(req.getPatientAddress())
            .notes(req.getNotes())
            .rejectionReason(req.getRejectionReason())
            .estimatedArrivalMinutes(req.getEstimatedArrivalMinutes())
            .actualArrivalTime(req.getActualArrivalTime())
            .completedAt(req.getCompletedAt())
            .isEmergency(req.getIsEmergency())
            .createdAt(req.getCreatedAt())
            .updatedAt(req.getUpdatedAt())
            .build();
}
}

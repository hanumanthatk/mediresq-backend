package com.smartemergency.service;

import com.smartemergency.dto.request.BedUpdateRequest;
import com.smartemergency.dto.request.HospitalRegisterRequest;
import com.smartemergency.dto.response.AmbulanceResponse;
import com.smartemergency.dto.response.BedResponse;
import com.smartemergency.dto.response.HospitalResponse;
import com.smartemergency.entity.*;
import com.smartemergency.enums.AmbulanceStatus;
import com.smartemergency.exception.BusinessException;
import com.smartemergency.exception.ResourceNotFoundException;
import com.smartemergency.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Hospital Service - manages hospital registration, bed management, ambulance operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HospitalService {

    private final HospitalRepository hospitalRepository;
    private final BedRepository bedRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // =====================================================
    // HOSPITAL PROFILE MANAGEMENT
    // =====================================================

    public HospitalResponse registerHospitalProfile(Long userId, HospitalRegisterRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Check if profile already exists
        if (hospitalRepository.findByUser(user).isPresent()) {
            throw new BusinessException("Hospital profile already registered for this account");
        }

        Hospital hospital = Hospital.builder()
                .user(user)
                .name(request.getName())
                .registrationNumber(request.getRegistrationNumber())
                .type(request.getType())
                .specialization(request.getSpecialization())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .phone(request.getPhone())
                .emergencyPhone(request.getEmergencyPhone())
                .email(request.getEmail())
                .website(request.getWebsite())
                .totalBeds(request.getTotalBeds())
                .totalIcuBeds(request.getTotalIcuBeds())
                .description(request.getDescription())
                .establishedYear(request.getEstablishedYear())
                .isActive(true)
                .isVerified(false) // Needs admin verification
                .build();

        Hospital saved = hospitalRepository.save(hospital);
        log.info("Hospital profile registered: {}", saved.getName());
        return toHospitalResponse(saved, null);
    }
    @Transactional(readOnly = true)
    public HospitalResponse getHospitalByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Hospital hospital = hospitalRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital profile not found for user: " + userId));
        return toHospitalResponse(hospital, null);
    }
     @Transactional(readOnly = true)
    public HospitalResponse getHospitalById(Long hospitalId) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", hospitalId));
        return toHospitalResponse(hospital, null);
    }
     @Transactional(readOnly = true)
    public List<HospitalResponse> getAllActiveHospitals() {
        return hospitalRepository.findByIsVerifiedAndIsActive(true, true)
                .stream()
                .map(h -> toHospitalResponse(h, null))
                .collect(Collectors.toList());
    }

    public List<HospitalResponse> findNearbyHospitals(double lat, double lng, double radiusKm) {
        return hospitalRepository.findNearbyHospitals(lat, lng, radiusKm)
                .stream()
                .map(h -> {
                    double distance = calculateDistance(lat, lng, h.getLatitude(), h.getLongitude());
                    HospitalResponse resp = toHospitalResponse(h, distance);
                    return resp;
                })
                .collect(Collectors.toList());
    }

    // =====================================================
    // BED MANAGEMENT
    // =====================================================

    @Transactional
    public BedResponse updateBedAvailability(Long hospitalId, BedUpdateRequest request) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", hospitalId));

        // Validate counts
        if (request.getAvailableCount() > request.getTotalCount()) {
            throw new BusinessException("Available count cannot exceed total count");
        }

        Optional<Bed> existingBed = bedRepository.findByHospitalAndBedType(hospital, request.getBedType());
        Bed bed;

        if (existingBed.isPresent()) {
            bed = existingBed.get();
            bed.setTotalCount(request.getTotalCount());
            bed.setAvailableCount(request.getAvailableCount());
            bed.setUnderMaintenance(request.getUnderMaintenance() != null ? request.getUnderMaintenance() : 0);
            bed.setOccupiedCount(request.getTotalCount() - request.getAvailableCount() -
                    (request.getUnderMaintenance() != null ? request.getUnderMaintenance() : 0));
            if (request.getCostPerDay() != null) bed.setCostPerDay(request.getCostPerDay());
            if (request.getNotes() != null) bed.setNotes(request.getNotes());
        } else {
            bed = Bed.builder()
                    .hospital(hospital)
                    .bedType(request.getBedType())
                    .totalCount(request.getTotalCount())
                    .availableCount(request.getAvailableCount())
                    .underMaintenance(request.getUnderMaintenance() != null ? request.getUnderMaintenance() : 0)
                    .occupiedCount(request.getTotalCount() - request.getAvailableCount())
                    .costPerDay(request.getCostPerDay())
                    .notes(request.getNotes())
                    .build();
        }

        Bed saved = bedRepository.save(bed);
        log.info("Bed availability updated for hospital: {} | Type: {} | Available: {}",
                hospital.getName(), request.getBedType(), request.getAvailableCount());

        // Broadcast real-time update via WebSocket
        broadcastBedUpdate(hospitalId, saved);

        return toBedResponse(saved);
    }
    @Transactional(readOnly = true)
    public List<BedResponse> getBedsByHospital(Long hospitalId) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", hospitalId));
        return bedRepository.findByHospital(hospital)
                .stream()
                .map(this::toBedResponse)
                .collect(Collectors.toList());
    }

    // =====================================================
    // AMBULANCE MANAGEMENT
    // =====================================================

    public List<AmbulanceResponse> getAmbulancesByHospital(Long hospitalId) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", hospitalId));
        return ambulanceRepository.findByHospital(hospital)
                .stream()
                .map(this::toAmbulanceResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AmbulanceResponse addAmbulance(Long hospitalId, Ambulance ambulance) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", hospitalId));
        ambulance.setHospital(hospital);
        return toAmbulanceResponse(ambulanceRepository.save(ambulance));
    }

    @Transactional
    public AmbulanceResponse updateAmbulanceStatus(Long ambulanceId, AmbulanceStatus newStatus) {
        Ambulance ambulance = ambulanceRepository.findById(ambulanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Ambulance", ambulanceId));
        ambulance.setStatus(newStatus);
        return toAmbulanceResponse(ambulanceRepository.save(ambulance));
    }

    // =====================================================
    // ADMIN OPERATIONS
    // =====================================================

    @Transactional
    public HospitalResponse verifyHospital(Long hospitalId, boolean verified) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", hospitalId));
        hospital.setIsVerified(verified);
        Hospital saved = hospitalRepository.save(hospital);
        log.info("Hospital {} verification status: {}", hospital.getName(), verified);
        return toHospitalResponse(saved, null);
    }

    public List<HospitalResponse> getAllHospitals() {
        return hospitalRepository.findAll()
                .stream()
                .map(h -> toHospitalResponse(h, null))
                .collect(Collectors.toList());
    }

    // =====================================================
    // PRIVATE HELPERS
    // =====================================================

    private void broadcastBedUpdate(Long hospitalId, Bed bed) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/hospital/" + hospitalId + "/beds",
                    toBedResponse(bed)
            );
        } catch (Exception e) {
            log.warn("Failed to broadcast bed update: {}", e.getMessage());
        }
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        if (lat2 == 0 || lng2 == 0) return 0;
        final int R = 6371; // Earth's radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(R * c * 100.0) / 100.0;
    }
    @Transactional(readOnly = true)
    public HospitalResponse toHospitalResponse(Hospital hospital, Double distance) {
        long availableAmbulances = ambulanceRepository.countByHospitalAndStatus(hospital, AmbulanceStatus.AVAILABLE);
        List<BedResponse> beds = bedRepository.findByHospital(hospital)
                .stream().map(this::toBedResponse).collect(Collectors.toList());

        return HospitalResponse.builder()
                .id(hospital.getId())
                .name(hospital.getName())
                .registrationNumber(hospital.getRegistrationNumber())
                .type(hospital.getType())
                .specialization(hospital.getSpecialization())
                .address(hospital.getAddress())
                .city(hospital.getCity())
                .state(hospital.getState())
                .pincode(hospital.getPincode())
                .latitude(hospital.getLatitude())
                .longitude(hospital.getLongitude())
                .phone(hospital.getPhone())
                .emergencyPhone(hospital.getEmergencyPhone())
                .email(hospital.getEmail())
                .website(hospital.getWebsite())
                .totalBeds(hospital.getTotalBeds())
                .totalIcuBeds(hospital.getTotalIcuBeds())
                .isVerified(hospital.getIsVerified())
                .isActive(hospital.getIsActive())
                .rating(hospital.getRating())
                .establishedYear(hospital.getEstablishedYear())
                .description(hospital.getDescription())
                .imageUrl(hospital.getImageUrl())
                .distanceKm(distance)
                .beds(beds)
                .availableAmbulances(availableAmbulances)
                .build();
    }

    private BedResponse toBedResponse(Bed bed) {
    Long hospitalId = null;
    String hospitalName = null;
    try {
        hospitalId = bed.getHospital().getId();
        hospitalName = bed.getHospital().getName();
    } catch (Exception e) {
        hospitalId = null;
        hospitalName = null;
    }

    return BedResponse.builder()
            .id(bed.getId())
            .hospitalId(hospitalId)
            .hospitalName(hospitalName)
            .bedType(bed.getBedType())
            .totalCount(bed.getTotalCount())
            .availableCount(bed.getAvailableCount())
            .occupiedCount(bed.getOccupiedCount())
            .underMaintenance(bed.getUnderMaintenance())
            .costPerDay(bed.getCostPerDay())
            .lastUpdated(bed.getLastUpdated())
            .notes(bed.getNotes())
            .build();
}

    private AmbulanceResponse toAmbulanceResponse(Ambulance ambulance) {
        return AmbulanceResponse.builder()
                .id(ambulance.getId())
                .hospitalId(ambulance.getHospital().getId())
                .hospitalName(ambulance.getHospital().getName())
                .vehicleNumber(ambulance.getVehicleNumber())
                .ambulanceType(ambulance.getAmbulanceType())
                .driverName(ambulance.getDriverName())
                .driverPhone(ambulance.getDriverPhone())
                .paramedicName(ambulance.getParamedicName())
                .status(ambulance.getStatus())
                .currentLatitude(ambulance.getCurrentLatitude())
                .currentLongitude(ambulance.getCurrentLongitude())
                .isActive(ambulance.getIsActive())
                .build();
    }
}

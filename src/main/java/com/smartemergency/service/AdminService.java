package com.smartemergency.service;

import com.smartemergency.dto.response.DashboardStats;
import com.smartemergency.dto.response.HospitalResponse;
import com.smartemergency.dto.response.UserResponse;
import com.smartemergency.entity.User;
import com.smartemergency.enums.BedType;
import com.smartemergency.enums.RequestStatus;
import com.smartemergency.enums.Role;
import com.smartemergency.exception.ResourceNotFoundException;
import com.smartemergency.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Service - provides system-wide management and analytics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final BedRepository bedRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final EmergencyRequestRepository emergencyRepo;
    private final HospitalService hospitalService;

    // =====================================================
    // DASHBOARD ANALYTICS
    // =====================================================
    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats() {
        Long availableIcu = bedRepository.sumAvailableByType(BedType.ICU);
        Long availableGeneral = bedRepository.sumAvailableByType(BedType.GENERAL);
        long totalUsers = userRepository.count();
        long totalPatients = userRepository.countByRole(Role.PATIENT);
        long totalHospitals = hospitalRepository.count();
        long activeHospitals = hospitalRepository.countActiveVerifiedHospitals();
        long pendingRequests = emergencyRepo.countByStatus(RequestStatus.PENDING);
        long completedRequests = emergencyRepo.countByStatus(RequestStatus.COMPLETED);
        long totalRequests = emergencyRepo.count();
        long todayRequests = emergencyRepo.countRequestsSince(LocalDateTime.now().toLocalDate().atStartOfDay());

        return DashboardStats.builder()
                .totalHospitals(totalHospitals)
                .activeHospitals(activeHospitals)
                .totalUsers(totalUsers)
                .totalPatients(totalPatients)
                .availableGeneralBeds(availableGeneral != null ? availableGeneral : 0L)
                .availableIcuBeds(availableIcu != null ? availableIcu : 0L)
                .totalRequests(totalRequests)
                .pendingRequests(pendingRequests)
                .completedRequests(completedRequests)
                .todayRequests(todayRequests)
                .build();
    }

    // =====================================================
    // USER MANAGEMENT
    // =====================================================
     @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getUsersByRole(Role role) {
        return userRepository.findByRole(role)
                .stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setIsActive(!user.getIsActive());
        log.info("User {} status toggled to: {}", user.getEmail(), user.getIsActive());
        return toUserResponse(userRepository.save(user));
    }

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return toUserResponse(user);
    }

    // =====================================================
    // HOSPITAL MANAGEMENT
    // =====================================================
    @Transactional(readOnly = true)
    public List<HospitalResponse> getAllHospitals() {
        return hospitalRepository.findAll()
                .stream()
                .map(h -> hospitalService.toHospitalResponse(h, null))
                .collect(Collectors.toList());
    }

    @Transactional
    public HospitalResponse verifyHospital(Long hospitalId, boolean verified) {
        return hospitalService.verifyHospital(hospitalId, verified);
    }

    @Transactional
    public HospitalResponse toggleHospitalStatus(Long hospitalId) {
        var hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital", hospitalId));
        hospital.setIsActive(!hospital.getIsActive());
        log.info("Hospital {} status toggled to: {}", hospital.getName(), hospital.getIsActive());
        return hospitalService.toHospitalResponse(hospitalRepository.save(hospital), null);
    }

    // =====================================================
    // PRIVATE HELPERS
    // =====================================================

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .isVerified(user.getIsVerified())
                .address(user.getAddress())
                .bloodGroup(user.getBloodGroup())
                .emergencyContact(user.getEmergencyContact())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

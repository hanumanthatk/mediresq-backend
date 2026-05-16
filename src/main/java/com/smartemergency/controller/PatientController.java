package com.smartemergency.controller;

import com.smartemergency.dto.request.EmergencyRequestDto;
import com.smartemergency.dto.response.EmergencyRequestResponse;
import com.smartemergency.dto.response.HospitalResponse;
import com.smartemergency.dto.response.NotificationResponse;
import com.smartemergency.dto.response.UserResponse;
import com.smartemergency.entity.User;
import com.smartemergency.enums.RequestStatus;
import com.smartemergency.service.EmergencyService;
import com.smartemergency.service.HospitalService;
import com.smartemergency.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Patient Controller - handles all patient-facing operations.
 * Base URL: /api/patient
 * Required Role: PATIENT
 */
@RestController
@RequestMapping("/patient")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PATIENT', 'ADMIN')")
public class PatientController {

    private final HospitalService hospitalService;
    private final EmergencyService emergencyService;
    private final NotificationService notificationService;

    // =====================================================
    // PROFILE
    // =====================================================

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(UserResponse.builder()
                .id(currentUser.getId())
                .firstName(currentUser.getFirstName())
                .lastName(currentUser.getLastName())
                .email(currentUser.getEmail())
                .phone(currentUser.getPhone())
                .role(currentUser.getRole())
                .isActive(currentUser.getIsActive())
                .address(currentUser.getAddress())
                .bloodGroup(currentUser.getBloodGroup())
                .emergencyContact(currentUser.getEmergencyContact())
                .createdAt(currentUser.getCreatedAt())
                .build());
    }

    // =====================================================
    // HOSPITAL SEARCH
    // =====================================================

    /**
     * GET /api/patient/hospitals/nearby
     * Find hospitals near patient's location using Haversine formula.
     * Query params: lat, lng, radius (default 10 km)
     */
    @GetMapping("/hospitals/nearby")
    public ResponseEntity<List<HospitalResponse>> findNearbyHospitals(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double radius) {
        return ResponseEntity.ok(hospitalService.findNearbyHospitals(lat, lng, radius));
    }

    /**
     * GET /api/patient/hospitals
     * Get all active verified hospitals
     */
    @GetMapping("/hospitals")
    public ResponseEntity<List<HospitalResponse>> getAllHospitals() {
        return ResponseEntity.ok(hospitalService.getAllActiveHospitals());
    }

    /**
     * GET /api/patient/hospitals/{id}
     * Get specific hospital details with live bed availability
     */
    @GetMapping("/hospitals/{hospitalId}")
    public ResponseEntity<HospitalResponse> getHospital(@PathVariable Long hospitalId) {
        return ResponseEntity.ok(hospitalService.getHospitalById(hospitalId));
    }

    // =====================================================
    // EMERGENCY REQUESTS
    // =====================================================

    /**
     * POST /api/patient/emergency/request
     * Create a new emergency request (SOS, AMBULANCE, BED_RESERVATION)
     */
    @PostMapping("/emergency/request")
    public ResponseEntity<EmergencyRequestResponse> createEmergencyRequest(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody EmergencyRequestDto dto) {
        return ResponseEntity.ok(emergencyService.createRequest(currentUser.getId(), dto));
    }

    /**
     * POST /api/patient/emergency/sos
     * Quick SOS emergency request with minimal data
     */
    @PostMapping("/emergency/sos")
    public ResponseEntity<EmergencyRequestResponse> sendSOS(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) String address) {

        EmergencyRequestDto dto = new EmergencyRequestDto();
        dto.setRequestType(com.smartemergency.enums.RequestType.SOS);
        dto.setPriority(com.smartemergency.enums.Priority.CRITICAL);
        dto.setPatientLatitude(lat);
        dto.setPatientLongitude(lng);
        dto.setPatientAddress(address);
        dto.setIsEmergency(true);
        dto.setPatientCondition("SOS - Emergency assistance required immediately");

        return ResponseEntity.ok(emergencyService.createRequest(currentUser.getId(), dto));
    }

    /**
     * GET /api/patient/emergency/requests
     * Get patient's request history
     */
    @GetMapping("/emergency/requests")
    public ResponseEntity<List<EmergencyRequestResponse>> getMyRequests(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(emergencyService.getPatientRequests(currentUser.getId()));
    }

    /**
     * GET /api/patient/emergency/requests/{id}
     * Track a specific request
     */
    @GetMapping("/emergency/requests/{requestId}")
    public ResponseEntity<EmergencyRequestResponse> trackRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(emergencyService.getRequestById(requestId));
    }

    /**
     * PATCH /api/patient/emergency/requests/{id}/cancel
     * Cancel a pending request
     */
    @PatchMapping("/emergency/requests/{requestId}/cancel")
    public ResponseEntity<EmergencyRequestResponse> cancelRequest(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long requestId) {

        com.smartemergency.dto.request.StatusUpdateRequest req =
                new com.smartemergency.dto.request.StatusUpdateRequest();
        req.setStatus(RequestStatus.CANCELLED);
        req.setNotes("Cancelled by patient");

        return ResponseEntity.ok(emergencyService.cancelByPatient(requestId, currentUser.getId()));
    }

    // =====================================================
    // NOTIFICATIONS
    // =====================================================

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(notificationService.getNotifications(currentUser.getId()));
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal User currentUser) {
        long count = notificationService.getUnreadCount(currentUser.getId());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PatchMapping("/notifications/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal User currentUser) {
        notificationService.markAllAsRead(currentUser.getId());
        return ResponseEntity.ok().build();
    }
}

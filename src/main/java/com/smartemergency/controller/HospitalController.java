package com.smartemergency.controller;

import com.smartemergency.dto.request.BedUpdateRequest;
import com.smartemergency.dto.request.HospitalRegisterRequest;
import com.smartemergency.dto.request.StatusUpdateRequest;
import com.smartemergency.dto.response.*;
import com.smartemergency.entity.User;
import com.smartemergency.service.EmergencyService;
import com.smartemergency.service.HospitalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Hospital Controller - manages hospital profiles, beds, ambulances, and emergency requests.
 *
 * Base URL: /api/hospital
 * Required Role: HOSPITAL or ADMIN
 */
@RestController
@RequestMapping("/hospital")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HOSPITAL', 'ADMIN')")
public class HospitalController {

    private final HospitalService hospitalService;
    private final EmergencyService emergencyService;

    // =====================================================
    // HOSPITAL PROFILE
    // =====================================================

    @PostMapping("/profile")
    public ResponseEntity<HospitalResponse> registerProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody HospitalRegisterRequest request) {
        return ResponseEntity.ok(hospitalService.registerHospitalProfile(currentUser.getId(), request));
    }

    @GetMapping("/profile")
    public ResponseEntity<HospitalResponse> getMyProfile(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(hospitalService.getHospitalByUserId(currentUser.getId()));
    }

    // =====================================================
    // BED MANAGEMENT
    // =====================================================

    @GetMapping("/beds")
    public ResponseEntity<List<BedResponse>> getBeds(@AuthenticationPrincipal User currentUser) {
        HospitalResponse hospital = hospitalService.getHospitalByUserId(currentUser.getId());
        return ResponseEntity.ok(hospitalService.getBedsByHospital(hospital.getId()));
    }

    @PutMapping("/beds")
    public ResponseEntity<BedResponse> updateBeds(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody BedUpdateRequest request) {
        HospitalResponse hospital = hospitalService.getHospitalByUserId(currentUser.getId());
        return ResponseEntity.ok(hospitalService.updateBedAvailability(hospital.getId(), request));
    }

    // =====================================================
    // AMBULANCE MANAGEMENT
    // =====================================================

    @GetMapping("/ambulances")
    public ResponseEntity<List<AmbulanceResponse>> getAmbulances(@AuthenticationPrincipal User currentUser) {
        HospitalResponse hospital = hospitalService.getHospitalByUserId(currentUser.getId());
        return ResponseEntity.ok(hospitalService.getAmbulancesByHospital(hospital.getId()));
    }

    @PatchMapping("/ambulances/{ambulanceId}/status")
    public ResponseEntity<AmbulanceResponse> updateAmbulanceStatus(
            @PathVariable Long ambulanceId,
            @RequestParam String status) {
        return ResponseEntity.ok(
            hospitalService.updateAmbulanceStatus(ambulanceId,
                com.smartemergency.enums.AmbulanceStatus.valueOf(status.toUpperCase()))
        );
    }

    // =====================================================
    // EMERGENCY REQUEST MANAGEMENT
    // =====================================================

    @GetMapping("/requests/active")
    public ResponseEntity<List<EmergencyRequestResponse>> getActiveRequests(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(emergencyService.getHospitalActiveRequests(currentUser.getId()));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<EmergencyRequestResponse>> getAllRequests(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(emergencyService.getAllHospitalRequests(currentUser.getId()));
    }

    @PatchMapping("/requests/{requestId}/status")
    public ResponseEntity<EmergencyRequestResponse> updateRequestStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long requestId,
            @Valid @RequestBody StatusUpdateRequest statusRequest) {
        return ResponseEntity.ok(
            emergencyService.updateRequestStatus(requestId, currentUser.getId(), statusRequest)
        );
    }

    // =====================================================
    // DASHBOARD SUMMARY
    // =====================================================

    @GetMapping("/dashboard")
    public ResponseEntity<HospitalResponse> getDashboard(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(hospitalService.getHospitalByUserId(currentUser.getId()));
    }
}

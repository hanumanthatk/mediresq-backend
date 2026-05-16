package com.smartemergency.controller;

import com.smartemergency.dto.response.*;
import com.smartemergency.enums.Role;
import com.smartemergency.service.AdminService;
import com.smartemergency.service.EmergencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin Controller - system-wide management, analytics, and oversight.
 * Base URL: /api/admin
 * Required Role: ADMIN only
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final EmergencyService emergencyService;

    // =====================================================
    // DASHBOARD ANALYTICS
    // =====================================================

    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStats> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // =====================================================
    // USER MANAGEMENT
    // =====================================================

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.getUserById(userId));
    }

    @GetMapping("/users/role/{role}")
    public ResponseEntity<List<UserResponse>> getUsersByRole(@PathVariable String role) {
        return ResponseEntity.ok(adminService.getUsersByRole(Role.valueOf(role.toUpperCase())));
    }

    @PatchMapping("/users/{userId}/toggle-status")
    public ResponseEntity<UserResponse> toggleUserStatus(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.toggleUserStatus(userId));
    }

    // =====================================================
    // HOSPITAL MANAGEMENT
    // =====================================================

    @GetMapping("/hospitals")
    public ResponseEntity<List<HospitalResponse>> getAllHospitals() {
        return ResponseEntity.ok(adminService.getAllHospitals());
    }

    @PatchMapping("/hospitals/{hospitalId}/verify")
    public ResponseEntity<HospitalResponse> verifyHospital(
            @PathVariable Long hospitalId,
            @RequestParam boolean verified) {
        return ResponseEntity.ok(adminService.verifyHospital(hospitalId, verified));
    }

    @PatchMapping("/hospitals/{hospitalId}/toggle-status")
    public ResponseEntity<HospitalResponse> toggleHospitalStatus(@PathVariable Long hospitalId) {
        return ResponseEntity.ok(adminService.toggleHospitalStatus(hospitalId));
    }

    // =====================================================
    // EMERGENCY REQUEST MONITORING
    // =====================================================

    @GetMapping("/requests")
    public ResponseEntity<List<EmergencyRequestResponse>> getAllRequests() {
        return ResponseEntity.ok(emergencyService.getAllRequests());
    }

    @GetMapping("/requests/{requestId}")
    public ResponseEntity<EmergencyRequestResponse> getRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(emergencyService.getRequestById(requestId));
    }
}

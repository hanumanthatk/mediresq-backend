package com.smartemergency.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStats {
    // General stats
    private Long totalHospitals;
    private Long activeHospitals;
    private Long totalUsers;
    private Long totalPatients;

    // Bed stats
    private Long totalBeds;
    private Long availableGeneralBeds;
    private Long availableIcuBeds;

    // Request stats
    private Long totalRequests;
    private Long pendingRequests;
    private Long activeRequests;
    private Long completedRequests;
    private Long todayRequests;

    // Ambulance stats
    private Long availableAmbulances;
    private Long totalAmbulances;
}

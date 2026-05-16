package com.smartemergency.dto.response;

import com.smartemergency.enums.Priority;
import com.smartemergency.enums.RequestStatus;
import com.smartemergency.enums.RequestType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class EmergencyRequestResponse {
    private Long id;
    private String requestNumber;
    private Long patientId;
    private String patientName;
    private String patientPhone;
    private Long hospitalId;
    private String hospitalName;
    private String hospitalPhone;
    private Long ambulanceId;
    private String ambulanceNumber;
    private RequestType requestType;
    private Priority priority;
    private RequestStatus status;
    private String patientCondition;
    private String symptoms;
    private Double patientLatitude;
    private Double patientLongitude;
    private String patientAddress;
    private String notes;
    private String rejectionReason;
    private Integer estimatedArrivalMinutes;
    private LocalDateTime actualArrivalTime;
    private LocalDateTime completedAt;
    private Boolean isEmergency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

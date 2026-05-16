package com.smartemergency.dto.request;

import com.smartemergency.enums.RequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusUpdateRequest {
    @NotNull(message = "Status is required")
    private RequestStatus status;
    private String notes;
    private String rejectionReason;
    private Long ambulanceId;
    private Integer estimatedArrivalMinutes;
}

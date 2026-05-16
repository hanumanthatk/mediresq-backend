package com.smartemergency.dto.request;

import com.smartemergency.enums.Priority;
import com.smartemergency.enums.RequestType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmergencyRequestDto {
    @NotNull(message = "Request type is required")
    private RequestType requestType;

    private Priority priority = Priority.HIGH;
    private Long hospitalId;
    private String patientCondition;
    private String symptoms;
    private Double patientLatitude;
    private Double patientLongitude;
    private String patientAddress;
    private String notes;
    private Boolean isEmergency = false;
}

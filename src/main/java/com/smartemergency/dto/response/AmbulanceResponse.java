package com.smartemergency.dto.response;

import com.smartemergency.enums.AmbulanceStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AmbulanceResponse {
    private Long id;
    private Long hospitalId;
    private String hospitalName;
    private String vehicleNumber;
    private String ambulanceType;
    private String driverName;
    private String driverPhone;
    private String paramedicName;
    private AmbulanceStatus status;
    private Double currentLatitude;
    private Double currentLongitude;
    private Boolean isActive;
}

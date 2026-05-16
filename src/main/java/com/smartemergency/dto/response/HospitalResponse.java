package com.smartemergency.dto.response;

import com.smartemergency.enums.HospitalType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class HospitalResponse {
    private Long id;
    private String name;
    private String registrationNumber;
    private HospitalType type;
    private String specialization;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private Double latitude;
    private Double longitude;
    private String phone;
    private String emergencyPhone;
    private String email;
    private String website;
    private Integer totalBeds;
    private Integer totalIcuBeds;
    private Boolean isVerified;
    private Boolean isActive;
    private BigDecimal rating;
    private Integer establishedYear;
    private String description;
    private String imageUrl;
    private Double distanceKm;
    private List<BedResponse> beds;
    private Long availableAmbulances;
}

package com.smartemergency.dto.request;

import com.smartemergency.enums.HospitalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HospitalRegisterRequest {
    @NotBlank(message = "Hospital name is required")
    private String name;

    @NotBlank(message = "Registration number is required")
    private String registrationNumber;

    @NotNull(message = "Hospital type is required")
    private HospitalType type;

    private String specialization;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    private String pincode;
private String state;

    private Double latitude;
    private Double longitude;

    @NotBlank(message = "Phone is required")
    private String phone;

    private String emergencyPhone;
    private String email;
    private String website;
    private Integer totalBeds;
    private Integer totalIcuBeds;
    private String description;
    private Integer establishedYear;
}

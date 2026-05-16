package com.smartemergency.dto.request;

import com.smartemergency.enums.BedType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class BedUpdateRequest {
    @NotNull(message = "Bed type is required")
    private BedType bedType;

    @NotNull(message = "Total count is required")
    @Min(value = 0, message = "Total count cannot be negative")
    private Integer totalCount;

    @NotNull(message = "Available count is required")
    @Min(value = 0, message = "Available count cannot be negative")
    private Integer availableCount;

    @Min(value = 0, message = "Maintenance count cannot be negative")
    private Integer underMaintenance = 0;

    private BigDecimal costPerDay;
    private String notes;
}

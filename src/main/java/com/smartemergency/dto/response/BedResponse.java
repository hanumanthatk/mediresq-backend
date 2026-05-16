package com.smartemergency.dto.response;

import com.smartemergency.enums.BedType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BedResponse {
    private Long id;
    private Long hospitalId;
    private String hospitalName;
    private BedType bedType;
    private Integer totalCount;
    private Integer availableCount;
    private Integer occupiedCount;
    private Integer underMaintenance;
    private BigDecimal costPerDay;
    private LocalDateTime lastUpdated;
    private String notes;
}

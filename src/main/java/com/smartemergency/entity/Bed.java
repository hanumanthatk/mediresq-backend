package com.smartemergency.entity;

import com.smartemergency.enums.BedType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "beds", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"hospital_id", "bed_type"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Enumerated(EnumType.STRING)
    @Column(name = "bed_type", nullable = false)
    private BedType bedType;

    @Column(name = "total_count")
    @Builder.Default
    private Integer totalCount = 0;

    @Column(name = "available_count")
    @Builder.Default
    private Integer availableCount = 0;

    @Column(name = "occupied_count")
    @Builder.Default
    private Integer occupiedCount = 0;

    @Column(name = "under_maintenance")
    @Builder.Default
    private Integer underMaintenance = 0;

    @Column(name = "cost_per_day", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal costPerDay = BigDecimal.ZERO;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
        // Auto-calculate occupied
        if (totalCount != null && availableCount != null) {
            occupiedCount = totalCount - availableCount - (underMaintenance != null ? underMaintenance : 0);
        }
    }
}

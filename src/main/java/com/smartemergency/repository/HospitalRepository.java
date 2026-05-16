package com.smartemergency.repository;

import com.smartemergency.entity.Hospital;
import com.smartemergency.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    Optional<Hospital> findByUser(User user);
    Optional<Hospital> findByRegistrationNumber(String registrationNumber);
    List<Hospital> findByIsVerifiedAndIsActive(Boolean isVerified, Boolean isActive);
    List<Hospital> findByCity(String city);
    List<Hospital> findByState(String state);

    /**
     * Finds hospitals within a specified radius using the Haversine formula.
     * Returns hospitals sorted by distance (nearest first).
     */
    @Query("""
        SELECT h FROM Hospital h
        WHERE h.isActive = true AND h.isVerified = true
        AND (6371 * acos(
            cos(radians(:lat)) * cos(radians(h.latitude)) *
            cos(radians(h.longitude) - radians(:lng)) +
            sin(radians(:lat)) * sin(radians(h.latitude))
        )) < :radiusKm
        ORDER BY (6371 * acos(
            cos(radians(:lat)) * cos(radians(h.latitude)) *
            cos(radians(h.longitude) - radians(:lng)) +
            sin(radians(:lat)) * sin(radians(h.latitude))
        ))
        """)
    List<Hospital> findNearbyHospitals(
        @Param("lat") double latitude,
        @Param("lng") double longitude,
        @Param("radiusKm") double radiusKm
    );

    @Query("SELECT COUNT(h) FROM Hospital h WHERE h.isVerified = true AND h.isActive = true")
    long countActiveVerifiedHospitals();
}

package com.smartemergency.repository;

import com.smartemergency.entity.EmergencyRequest;
import com.smartemergency.entity.Hospital;
import com.smartemergency.entity.User;
import com.smartemergency.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmergencyRequestRepository extends JpaRepository<EmergencyRequest, Long> {
    List<EmergencyRequest> findByPatient(User patient);
    List<EmergencyRequest> findByPatientOrderByCreatedAtDesc(User patient);
    List<EmergencyRequest> findByHospital(Hospital hospital);
    List<EmergencyRequest> findByHospitalAndStatus(Hospital hospital, RequestStatus status);
    List<EmergencyRequest> findByStatus(RequestStatus status);
    Page<EmergencyRequest> findByHospitalOrderByCreatedAtDesc(Hospital hospital, Pageable pageable);

    @Query("SELECT COUNT(r) FROM EmergencyRequest r WHERE r.status = :status")
    long countByStatus(@Param("status") RequestStatus status);

    @Query("SELECT COUNT(r) FROM EmergencyRequest r WHERE r.hospital = :hospital AND r.status = :status")
    long countByHospitalAndStatus(@Param("hospital") Hospital hospital, @Param("status") RequestStatus status);

    @Query("SELECT COUNT(r) FROM EmergencyRequest r WHERE r.createdAt >= :from")
    long countRequestsSince(@Param("from") LocalDateTime from);

    @Query("""
        SELECT r FROM EmergencyRequest r
        WHERE r.hospital = :hospital
        AND r.status IN ('PENDING', 'ACCEPTED', 'DISPATCHED', 'EN_ROUTE')
        ORDER BY r.priority ASC, r.createdAt ASC
        """)
    List<EmergencyRequest> findActiveRequestsByHospital(@Param("hospital") Hospital hospital);
}

package com.smartemergency.repository;

import com.smartemergency.entity.Bed;
import com.smartemergency.entity.Hospital;
import com.smartemergency.enums.BedType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BedRepository extends JpaRepository<Bed, Long> {
    List<Bed> findByHospital(Hospital hospital);
    Optional<Bed> findByHospitalAndBedType(Hospital hospital, BedType bedType);

    @Query("SELECT SUM(b.availableCount) FROM Bed b WHERE b.bedType = :type")
    Long sumAvailableByType(@Param("type") BedType type);

    @Query("SELECT SUM(b.availableCount) FROM Bed b WHERE b.hospital.id = :hospitalId")
    Integer sumAvailableByHospital(@Param("hospitalId") Long hospitalId);

    @Query("SELECT b FROM Bed b WHERE b.hospital.isActive = true AND b.availableCount > 0 AND b.bedType = :type")
    List<Bed> findAvailableByType(@Param("type") BedType type);
}

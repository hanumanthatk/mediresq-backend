package com.smartemergency.repository;

import com.smartemergency.entity.Ambulance;
import com.smartemergency.entity.Hospital;
import com.smartemergency.enums.AmbulanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AmbulanceRepository extends JpaRepository<Ambulance, Long> {
    List<Ambulance> findByHospital(Hospital hospital);
    List<Ambulance> findByHospitalAndStatus(Hospital hospital, AmbulanceStatus status);
    List<Ambulance> findByStatus(AmbulanceStatus status);
    long countByHospitalAndStatus(Hospital hospital, AmbulanceStatus status);
}

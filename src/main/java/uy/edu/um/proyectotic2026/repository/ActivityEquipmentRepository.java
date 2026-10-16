package uy.edu.um.proyectotic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.um.proyectotic2026.entities.ActivityEquipment;

public interface ActivityEquipmentRepository extends JpaRepository<ActivityEquipment, Long> {

    boolean existsByEquipmentId(Long equipmentId);
}

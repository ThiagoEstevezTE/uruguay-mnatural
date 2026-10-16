package uy.edu.um.proyectotic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.um.proyectotic2026.entities.ActivityInstance;
import uy.edu.um.proyectotic2026.entities.InstanceStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityInstanceRepository extends JpaRepository<ActivityInstance, Long> {

    // Proximas fechas de una actividad (sin las canceladas)
    List<ActivityInstance> findByActivityIdAndStatusAndStartDateTimeAfterOrderByStartDateTimeAsc(
            Long activityId, InstanceStatus status, LocalDateTime from);
}

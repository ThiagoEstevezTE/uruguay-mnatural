package uy.edu.um.proyectotic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.um.proyectotic2026.entities.ActivityPhoto;

import java.util.Optional;

public interface ActivityPhotoRepository extends JpaRepository<ActivityPhoto, Long> {

    Optional<ActivityPhoto> findByActivityIdAndPosition(Long activityId, int position);

    long countByActivityId(Long activityId);

    void deleteByActivityId(Long activityId);
}

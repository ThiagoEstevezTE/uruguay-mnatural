package uy.edu.um.proyectotic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.um.proyectotic2026.entities.Rating;

import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findByBookingInstanceActivityIdOrderByDateDesc(Long activityId);
}

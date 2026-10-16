package uy.edu.um.proyectotic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.um.proyectotic2026.entities.ActivityType;

import java.util.List;
import java.util.Optional;

public interface ActivityTypeRepository extends JpaRepository<ActivityType, Long> {

    List<ActivityType> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    Optional<ActivityType> findByName(String name);
}

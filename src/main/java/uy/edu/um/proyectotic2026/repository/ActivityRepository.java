package uy.edu.um.proyectotic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.um.proyectotic2026.entities.Activity;
import uy.edu.um.proyectotic2026.entities.BusinessStatus;

import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByBusinessProviderEmailOrderByNameAsc(String providerEmail);

    boolean existsByTypesId(Long typeId);

    // Las actividades que se pueden ver en el portal: publicadas y de un emprendimiento aprobado
    List<Activity> findByPublishedTrueAndBusinessStatusOrderByNameAsc(BusinessStatus status);
}


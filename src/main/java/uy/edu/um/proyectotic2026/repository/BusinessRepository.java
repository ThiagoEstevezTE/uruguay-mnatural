package uy.edu.um.proyectotic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.um.proyectotic2026.entities.Business;
import uy.edu.um.proyectotic2026.entities.BusinessStatus;

import java.util.List;

public interface BusinessRepository extends JpaRepository<Business, Long> {

    List<Business> findByProviderId(Long providerId);

    List<Business> findByStatus(BusinessStatus status);
}
package uy.edu.um.proyectotic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.um.proyectotic2026.entities.Tourist;

import java.util.Optional;

public interface TouristRepository extends JpaRepository<Tourist, Long> {

    boolean existsByDocument(String document);

    Optional<Tourist> findByEmail(String email);
}

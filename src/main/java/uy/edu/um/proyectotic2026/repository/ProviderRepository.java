package uy.edu.um.proyectotic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.um.proyectotic2026.entities.Provider;

import java.util.Optional;

public interface ProviderRepository extends JpaRepository<Provider, Long> {

    boolean existsByDocument(String document);

    Optional<Provider> findByEmail(String email);
}

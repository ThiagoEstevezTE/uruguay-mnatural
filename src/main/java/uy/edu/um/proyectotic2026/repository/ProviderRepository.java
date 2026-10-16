package uy.edu.um.proyectotic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.um.proyectotic2026.entities.Provider;

public interface ProviderRepository extends JpaRepository<Provider, Long> {

    boolean existsByDocument(String document);
}
package uy.edu.um.proyectotic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.um.proyectotic2026.entities.CancellationReason;

import java.util.List;
import java.util.Optional;

public interface CancellationReasonRepository extends JpaRepository<CancellationReason, Long> {

    List<CancellationReason> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    Optional<CancellationReason> findByName(String name);
}

package uy.edu.um.proyectotic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.um.proyectotic2026.entities.Refund;
import uy.edu.um.proyectotic2026.entities.RefundStatus;

import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByStatus(RefundStatus status);
}

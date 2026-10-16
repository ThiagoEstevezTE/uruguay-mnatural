package uy.edu.um.proyectotic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.um.proyectotic2026.entities.Locality;

import java.util.List;

public interface LocalityRepository extends JpaRepository<Locality, Long> {

    List<Locality> findByDepartmentIdOrderByNameAsc(Long departmentId);
}
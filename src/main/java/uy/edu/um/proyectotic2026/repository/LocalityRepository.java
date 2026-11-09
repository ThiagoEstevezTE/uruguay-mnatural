package uy.edu.um.proyectotic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uy.edu.um.proyectotic2026.entities.Locality;

import java.util.List;
import java.util.Optional;

public interface LocalityRepository extends JpaRepository<Locality, Long> {

    List<Locality> findByDepartmentIdOrderByNameAsc(Long departmentId);

    Optional<Locality> findByName(String name);

    // Todas con su departamento en una sola consulta (join fetch)
    @Query("select l from Locality l join fetch l.department d order by d.name, l.name")
    List<Locality> findAllWithDepartment();
}

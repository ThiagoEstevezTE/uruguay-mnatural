package uy.edu.um.proyectotic2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.um.proyectotic2026.entities.User;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);
}
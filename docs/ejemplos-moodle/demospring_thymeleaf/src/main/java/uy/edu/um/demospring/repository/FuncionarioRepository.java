package uy.edu.um.demospring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.um.demospring.entities.Funcionario;

import java.util.Optional;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    public Optional<Funcionario> findByNroFunc(int nroFunc);
}

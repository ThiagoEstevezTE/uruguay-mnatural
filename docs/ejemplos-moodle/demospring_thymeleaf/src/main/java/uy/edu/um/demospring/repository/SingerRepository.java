package uy.edu.um.demospring.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uy.edu.um.demospring.entities.Singer;

import java.util.Date;
import java.util.List;
import java.util.Optional;


public interface SingerRepository extends JpaRepository<Singer, Long>
{
    public Optional<Singer> findByFirstName(String firstName);

//    @Query(value = "select * from SINGER where BIRTH_DATE>=:fecha")
//    public List<Singer> cantantesNacidosDespuesDe(Date fecha);
}

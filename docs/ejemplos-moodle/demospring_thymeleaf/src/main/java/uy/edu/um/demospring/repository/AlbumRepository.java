package uy.edu.um.demospring.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import uy.edu.um.demospring.entities.Album;
import uy.edu.um.demospring.entities.Singer;

public interface AlbumRepository extends JpaRepository<Album, Long>
{

}

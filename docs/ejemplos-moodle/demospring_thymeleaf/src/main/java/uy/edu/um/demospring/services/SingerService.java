package uy.edu.um.demospring.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.thymeleaf.expression.Strings;
import uy.edu.um.demospring.entities.Singer;
import uy.edu.um.demospring.exceptions.EntityNotFoundException;
import uy.edu.um.demospring.exceptions.InvalidDataException;
import uy.edu.um.demospring.repository.SingerRepository;

@Service
public class SingerService
{
    @Autowired
    private SingerRepository singerRepo;

    public Singer addSinger(String name, String lastName, Date birthDate) throws InvalidDataException {
        if(name == null || lastName == null || birthDate == null)
        {
            throw new InvalidDataException("lOS DATOS DEL NOMBRE, O APELLIDO O FECHA DE NACIMIENTO NO SON CORRECTOS");
        }
        if(name.trim().equals("") || lastName.trim().equals(""))
        {
            throw new InvalidDataException("Alguno de los datos de nombre o apellido debe ser completado");
        }

        Singer unCantante = Singer.builder()
                .firstName(name)
                .lastName(lastName)
                .birthDate(birthDate)
                .build();
        return singerRepo.save(unCantante);
    }

    public List<Singer> todosLosCantantes()
    {
        return singerRepo.findAll();
    }

    public Singer porApellido(String apellido) throws InvalidDataException, EntityNotFoundException {
        if(apellido == null)
            throw new InvalidDataException("Valor de la variable null");
        Optional<Singer> result = singerRepo.findByFirstName(apellido);
        if(result.isEmpty())
            throw new EntityNotFoundException("Cantante no encontrado");
        Singer unCantante = result.get();
        if(unCantante.getId()<0)
            throw new EntityNotFoundException("Error al obtener la entidad");
        return unCantante;
    }

    public Optional<Singer> findById(Long id)
    {
        return singerRepo.findById(id);
    }

}

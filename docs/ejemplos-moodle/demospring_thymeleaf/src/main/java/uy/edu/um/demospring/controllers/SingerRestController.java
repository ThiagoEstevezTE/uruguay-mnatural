package uy.edu.um.demospring.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.function.EntityResponse;
import uy.edu.um.demospring.entities.Singer;
import uy.edu.um.demospring.exceptions.EntityNotFoundException;
import uy.edu.um.demospring.exceptions.InvalidDataException;
import uy.edu.um.demospring.services.SingerService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/singer")
public class SingerRestController {

    @Autowired
    private SingerService singerService;

        @GetMapping("/all")
    public ResponseEntity<List<Singer>> allSingers()
    {
        List<Singer> cantantes = singerService.todosLosCantantes();
        return ResponseEntity.ok(cantantes);
    }

    @GetMapping("/{apellido}")
    public ResponseEntity<Singer> cantantePorApellido(@PathVariable("apellido") String apellido)
    {

        try {
            Singer elCantante = singerService.porApellido(apellido);
            return ResponseEntity.ok(elCantante);
        } catch (InvalidDataException e) {
            return ResponseEntity.status(500).build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(404).build();
        }
    }
}

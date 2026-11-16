package uy.edu.um.proyectotic2026.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import uy.edu.um.proyectotic2026.entities.ActivityPhoto;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.services.ActivityPhotoService;

// Devuelve la foto de una actividad para mostrarla en las paginas
@Controller
public class ActivityPhotoController {

    @Autowired
    private ActivityPhotoService photoService;

    @GetMapping("/activities/{id}/photos/{position}")
    public ResponseEntity<byte[]> photo(@PathVariable("id") Long id, @PathVariable("position") int position) {
        try {
            ActivityPhoto photo = photoService.getPhoto(id, position);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(photo.getContentType()))
                    .body(photo.getData());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

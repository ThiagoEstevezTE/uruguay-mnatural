package uy.edu.um.proyectotic2026.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uy.edu.um.proyectotic2026.entities.Activity;
import uy.edu.um.proyectotic2026.entities.ActivityPhoto;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.repository.ActivityPhotoRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Fotos de las actividades: controla lo que sube el prestador, las guarda y las devuelve para mostrarlas
@Service
public class ActivityPhotoService {

    public static final int MAX_PHOTOS = 3;

    @Autowired
    private ActivityPhotoRepository photoRepo;

    // Controla los archivos elegidos y los devuelve como fotos sin guardar.
    // Si no se eligio ninguno, devuelve una lista vacia.
    public List<ActivityPhoto> check(List<MultipartFile> files) throws InvalidDataException {
        List<MultipartFile> chosen = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                // El navegador manda un archivo vacio si no se eligio nada
                if (file != null && !file.isEmpty())
                    chosen.add(file);
            }
        }

        if (chosen.size() > MAX_PHOTOS)
            throw new InvalidDataException("Podés subir hasta " + MAX_PHOTOS + " fotos");

        List<ActivityPhoto> photos = new ArrayList<>();
        for (MultipartFile file : chosen) {
            if (file.getSize() > ActivityPhoto.MAX_BYTES)
                throw new InvalidDataException("Cada foto puede pesar hasta 5 MB");

            byte[] data;
            try {
                data = file.getBytes();
            } catch (IOException e) {
                throw new InvalidDataException("No se pudo leer una de las fotos");
            }

            // El tipo sale del contenido del archivo: cambiarle la extension a otra cosa no sirve
            String contentType = imageType(data);
            if (contentType == null)
                throw new InvalidDataException("Las fotos tienen que ser JPG, PNG o WebP");

            photos.add(ActivityPhoto.builder().position(photos.size()).contentType(contentType).data(data).build());
        }
        return photos;
    }

    // Cambia las fotos de la actividad por las nuevas. Si no hay nuevas, quedan las que tenia.
    @Transactional
    public void replacePhotos(Activity activity, List<ActivityPhoto> photos) {
        if (photos.isEmpty())
            return;
        photoRepo.deleteByActivityId(activity.getId());
        photoRepo.flush();
        for (ActivityPhoto photo : photos)
            photo.setActivity(activity);
        photoRepo.saveAll(photos);
    }

    public ActivityPhoto getPhoto(Long activityId, int position) throws EntityNotFoundException {
        return photoRepo.findByActivityIdAndPosition(activityId, position)
                .orElseThrow(() -> new EntityNotFoundException("Foto no encontrada"));
    }

    // Reconoce el formato por los primeros bytes del archivo. null = no es una imagen aceptada.
    static String imageType(byte[] data) {
        if (startsWith(data, 0, 0xFF, 0xD8, 0xFF))
            return "image/jpeg";
        if (startsWith(data, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A))
            return "image/png";
        if (startsWith(data, 0, 'R', 'I', 'F', 'F') && startsWith(data, 8, 'W', 'E', 'B', 'P'))
            return "image/webp";
        return null;
    }

    private static boolean startsWith(byte[] data, int offset, int... expected) {
        if (data.length < offset + expected.length)
            return false;
        for (int i = 0; i < expected.length; i++) {
            if ((data[offset + i] & 0xFF) != expected[i])
                return false;
        }
        return true;
    }
}

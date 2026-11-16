package uy.edu.um.proyectotic2026.services;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import uy.edu.um.proyectotic2026.entities.ActivityPhoto;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Que solo se acepten fotos de verdad, hasta 3 y de hasta 5 MB
class ActivityPhotoServiceTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
    static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 0, 0};

    private final ActivityPhotoService service = new ActivityPhotoService();

    static MockMultipartFile file(String name, String type, byte[] data) {
        return new MockMultipartFile("photos", name, type, data);
    }

    @Test
    void acceptsJpgPngAndWebpInOrder() throws Exception {
        List<ActivityPhoto> photos = service.check(List.of(
                file("a.jpg", "image/jpeg", JPG), file("b.png", "image/png", PNG), file("c.webp", "image/webp", WEBP)));

        assertEquals(List.of("image/jpeg", "image/png", "image/webp"),
                photos.stream().map(ActivityPhoto::getContentType).toList());
        assertEquals(List.of(0, 1, 2), photos.stream().map(ActivityPhoto::getPosition).toList());
    }

    @Test
    void noFilesChosenIsEmpty() throws Exception {
        // Sin elegir nada, el navegador manda un archivo vacio
        assertTrue(service.check(List.of(file("", "application/octet-stream", new byte[0]))).isEmpty());
        assertTrue(service.check(null).isEmpty());
    }

    @Test
    void rejectsMoreThanThree() {
        List<MultipartFile> four = List.of(file("1.png", "image/png", PNG), file("2.png", "image/png", PNG),
                file("3.png", "image/png", PNG), file("4.png", "image/png", PNG));
        InvalidDataException e = assertThrows(InvalidDataException.class, () -> service.check(four));
        assertEquals("Podés subir hasta 3 fotos", e.getMessage());
    }

    @Test
    void rejectsOtherFileWithImageName() {
        // Un HTML con nombre y tipo de foto: el contenido lo delata
        MockMultipartFile fake = file("foto.jpg", "image/jpeg", "<html><script>alert(1)</script></html>".getBytes());
        InvalidDataException e = assertThrows(InvalidDataException.class, () -> service.check(List.of(fake)));
        assertEquals("Las fotos tienen que ser JPG, PNG o WebP", e.getMessage());
    }

    @Test
    void rejectsSvg() {
        MockMultipartFile svg = file("foto.svg", "image/svg+xml", "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes());
        assertThrows(InvalidDataException.class, () -> service.check(List.of(svg)));
    }

    @Test
    void rejectsPhotoOverFiveMegabytes() {
        byte[] big = new byte[ActivityPhoto.MAX_BYTES + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);
        InvalidDataException e = assertThrows(InvalidDataException.class,
                () -> service.check(List.of(file("grande.png", "image/png", big))));
        assertEquals("Cada foto puede pesar hasta 5 MB", e.getMessage());
    }
}

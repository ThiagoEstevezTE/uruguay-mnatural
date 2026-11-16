package uy.edu.um.proyectotic2026.entities;

import jakarta.persistence.*;
import lombok.*;

// Foto que sube el prestador para su actividad (hasta 3). Se guarda en la base,
// asi anda igual en Docker y en Supabase sin carpetas de archivos.
@Entity
@Table(name = "activity_photos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityPhoto {

    // Tamaño maximo de cada foto: 5 MB
    public static final int MAX_BYTES = 5 * 1024 * 1024;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    // Orden de la foto: 0 es la portada
    @Column(nullable = false)
    private int position;

    // image/jpeg, image/png o image/webp. Lo decide el servidor mirando el archivo, no el navegador.
    @Column(nullable = false, length = 20)
    private String contentType;

    // bytea: el tipo de Postgres para archivos (H2, la base de las pruebas, lo entiende igual)
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] data;
}

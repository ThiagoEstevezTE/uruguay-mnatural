package uy.edu.um.proyectotic2026.entities;

import jakarta.persistence.*;
import lombok.*;

// Categoria de actividad (senderismo, cabalgata...). La mantiene el administrador.
@Entity
@Table(name = "activity_types")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}

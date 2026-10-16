package uy.edu.um.proyectotic2026.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
// No puede haber dos localidades con el mismo nombre en un departamento
@Table(name = "localities", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "department_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Locality {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    // Coordenadas para el pronostico del clima (API publica Open-Meteo)
    private Double latitude;

    private Double longitude;
}
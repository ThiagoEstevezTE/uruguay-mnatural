package uy.edu.um.proyectotic2026.entities;

import jakarta.persistence.*;
import lombok.*;

// Tabla intermedia ACTIVIDAD_EQUIPAMIENTO del MER. Tiene un dato propio:
// si el equipamiento lo da el prestador o lo tiene que traer el turista.
@Entity
@Table(name = "activity_equipment")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityEquipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(nullable = false)
    private boolean providedByProvider;
}

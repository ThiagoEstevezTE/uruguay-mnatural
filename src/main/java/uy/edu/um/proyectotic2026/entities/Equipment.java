package uy.edu.um.proyectotic2026.entities;

import jakarta.persistence.*;
import lombok.*;

// Tipo de equipamiento (casco, chaleco salvavidas...). Lo mantiene el administrador.
@Entity
@Table(name = "equipment")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}

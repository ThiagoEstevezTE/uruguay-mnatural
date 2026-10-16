package uy.edu.um.proyectotic2026.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "activities")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Emprendimiento que la ofrece
    @ManyToOne
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    // Donde se realiza (puede ser distinto de donde esta el emprendimiento)
    @ManyToOne
    @JoinColumn(name = "locality_id", nullable = false)
    private Locality locality;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private int durationMinutes;

    // Precio por persona, en pesos uruguayos
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // null = sin edad minima
    private Integer minimumAge;

    // Minutos antes del inicio en que se cierran las reservas. 0 = hasta la hora de inicio.
    @Builder.Default
    @Column(nullable = false)
    private int bookingLimitMinutes = 0;

    // Cancelacion por parte del turista (Acta 1, punto 4.5)
    @Column(nullable = false)
    private boolean allowsCancellation;

    private Integer cancellationMinHours;

    @Builder.Default
    @Column(nullable = false)
    private boolean published = true;

    // Tabla intermedia ACTIVIDAD_TIPO del MER
    @ManyToMany
    @JoinTable(name = "activity_activity_types",
            joinColumns = @JoinColumn(name = "activity_id"),
            inverseJoinColumns = @JoinColumn(name = "activity_type_id"))
    @Builder.Default
    private Set<ActivityType> types = new HashSet<>();

    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ActivityEquipment> equipment = new ArrayList<>();

    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL)
    @OrderBy("startDateTime ASC")
    @Builder.Default
    private List<ActivityInstance> instances = new ArrayList<>();

    public void addEquipment(Equipment item, boolean providedByProvider) {
        equipment.add(ActivityEquipment.builder()
                .activity(this)
                .equipment(item)
                .providedByProvider(providedByProvider)
                .build());
    }

    public void addInstance(ActivityInstance instance) {
        instance.setActivity(this);
        instances.add(instance);
    }

    // Fechas en las que todavia se puede reservar: programadas y antes del cierre de reservas.
    // "instances" ya viene ordenada por fecha (@OrderBy).
    public List<ActivityInstance> getUpcomingInstances() {
        LocalDateTime now = LocalDateTime.now();
        return instances.stream()
                .filter(i -> i.getStatus() == InstanceStatus.SCHEDULED)
                .filter(i -> i.getBookingDeadline().isAfter(now))
                .toList();
    }

    // Nombres de los tipos en orden alfabetico (para mostrar etiquetas)
    public List<String> getTypeNames() {
        return types.stream().map(ActivityType::getName).sorted().toList();
    }

    // Duracion para mostrar: "3 h", "1 h 30 min", "45 min"
    public String getDurationText() {
        int hours = durationMinutes / 60;
        int minutes = durationMinutes % 60;
        if (hours == 0)
            return minutes + " min";
        if (minutes == 0)
            return hours + " h";
        return hours + " h " + minutes + " min";
    }
}

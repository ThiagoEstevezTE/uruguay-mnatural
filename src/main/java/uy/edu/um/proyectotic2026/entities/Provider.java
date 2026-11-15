package uy.edu.um.proyectotic2026.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "providers")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Provider extends User {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String document;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProviderType type;

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Business> businesses = new ArrayList<>();

    // Cada prestador puede tener un solo emprendimiento
    public void addBusiness(Business business) {
        if (!businesses.isEmpty())
            throw new IllegalStateException("Cada prestador puede tener un solo emprendimiento");
        business.setProvider(this);
        businesses.add(business);
    }
}
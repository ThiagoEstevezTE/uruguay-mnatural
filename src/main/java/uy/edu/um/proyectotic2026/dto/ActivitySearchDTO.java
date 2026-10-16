package uy.edu.um.proyectotic2026.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

// Filtros del buscador del portal. Todos son opcionales.
@Getter
@Setter
@NoArgsConstructor

public class ActivitySearchDTO {

    private String keyword;
    private Long departmentId;
    private Long typeId;
    private BigDecimal maxPrice;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

}

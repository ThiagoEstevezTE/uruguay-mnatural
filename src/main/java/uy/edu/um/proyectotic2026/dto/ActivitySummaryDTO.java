package uy.edu.um.proyectotic2026.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uy.edu.um.proyectotic2026.entities.Activity;

import java.math.BigDecimal;
import java.util.List;

// Como se ve una actividad en la API publica (no se devuelve la entidad)
@Getter
@AllArgsConstructor

public class ActivitySummaryDTO {

    private Long id;
    private String name;
    private String business;
    private String locality;
    private String department;
    private BigDecimal price;
    private int durationMinutes;
    private List<String> types;
    private Double averageRating;

    public static ActivitySummaryDTO from(Activity activity, Double averageRating) {
        return new ActivitySummaryDTO(
                activity.getId(),
                activity.getName(),
                activity.getBusiness().getName(),
                activity.getLocality().getName(),
                activity.getLocality().getDepartment().getName(),
                activity.getPrice(),
                activity.getDurationMinutes(),
                activity.getTypeNames(),
                averageRating
        );
    }
}

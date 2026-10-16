package uy.edu.um.proyectotic2026.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Pronostico de un dia, ya listo para mostrar
@Getter
@AllArgsConstructor

public class WeatherDTO {

    private String description;
    private String icon;              // icono de Bootstrap Icons
    private int maxTemperature;
    private int minTemperature;
    private Integer rainProbability;  // porcentaje

}

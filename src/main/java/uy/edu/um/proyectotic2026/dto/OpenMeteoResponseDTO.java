package uy.edu.um.proyectotic2026.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// Respuesta de la API de Open-Meteo. Solo se mapean los datos que se usan.
// Viene por columnas: daily.time[i] es el dia y daily.weatherCode[i] su clima.
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)

public class OpenMeteoResponseDTO {

    private Daily daily;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Daily {

        private List<String> time;

        @JsonProperty("weather_code")
        private List<Integer> weatherCode;

        @JsonProperty("temperature_2m_max")
        private List<Double> maxTemperature;

        @JsonProperty("temperature_2m_min")
        private List<Double> minTemperature;

        @JsonProperty("precipitation_probability_max")
        private List<Integer> rainProbability;
    }
}

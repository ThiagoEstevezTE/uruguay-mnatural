package uy.edu.um.proyectotic2026.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import uy.edu.um.proyectotic2026.dto.OpenMeteoResponseDTO;
import uy.edu.um.proyectotic2026.dto.WeatherDTO;
import uy.edu.um.proyectotic2026.entities.Locality;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Pronostico del clima para la fecha y el lugar de la actividad (lo sugiere la letra).
// Usa la API publica de Open-Meteo: es gratis y no necesita cuenta ni clave.
// Si no hay internet o la API falla, la pagina se muestra igual, sin el clima.
@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    // Open-Meteo da pronostico para los proximos 16 dias
    private static final int MAX_FORECAST_DAYS = 15;

    private final RestClient restClient;

    public WeatherService(@Value("${weather.api.url}") String apiUrl) {
        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(3000);
        timeouts.setReadTimeout(3000);

        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .requestFactory(timeouts)
                .build();
    }

    // Devuelve el pronostico de cada fecha pedida (las que tienen pronostico). Hace una sola consulta.
    public Map<LocalDate, WeatherDTO> getForecast(Locality locality, List<LocalDate> dates) {
        Map<LocalDate, WeatherDTO> result = new HashMap<>();

        LocalDate today = LocalDate.now();
        List<LocalDate> inRange = dates.stream()
                .filter(d -> !d.isBefore(today) && !d.isAfter(today.plusDays(MAX_FORECAST_DAYS)))
                .sorted()
                .toList();

        if (inRange.isEmpty() || locality.getLatitude() == null || locality.getLongitude() == null)
            return result;

        try {
            OpenMeteoResponseDTO response = restClient.get()
                    .uri(uri -> uri.path("/v1/forecast")
                            .queryParam("latitude", locality.getLatitude())
                            .queryParam("longitude", locality.getLongitude())
                            .queryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max")
                            .queryParam("timezone", "America/Montevideo")
                            .queryParam("start_date", inRange.get(0).toString())
                            .queryParam("end_date", inRange.get(inRange.size() - 1).toString())
                            .build())
                    .retrieve()
                    .body(OpenMeteoResponseDTO.class);

            if (response == null || response.getDaily() == null || response.getDaily().getTime() == null)
                return result;

            OpenMeteoResponseDTO.Daily daily = response.getDaily();
            for (int i = 0; i < daily.getTime().size(); i++) {
                LocalDate day = LocalDate.parse(daily.getTime().get(i));
                if (inRange.contains(day))
                    result.put(day, toWeather(daily.getWeatherCode().get(i), daily.getMaxTemperature().get(i),
                            daily.getMinTemperature().get(i), daily.getRainProbability().get(i)));
            }
        } catch (RestClientException e) {
            // Sin pronostico: no es un error para el turista, simplemente no se muestra
            log.warn("No se pudo obtener el pronóstico del clima: {}", e.getMessage());
        }
        return result;
    }

    // Traduce el codigo de clima de Open-Meteo (estandar WMO) a texto e icono
    public WeatherDTO toWeather(Integer code, Double max, Double min, Integer rain) {
        String description;
        String icon;
        int c = code == null ? -1 : code;

        if (c == 0) {
            description = "Despejado";
            icon = "bi-sun";
        } else if (c <= 3) {
            description = "Parcialmente nublado";
            icon = "bi-cloud-sun";
        } else if (c == 45 || c == 48) {
            description = "Niebla";
            icon = "bi-cloud-fog";
        } else if (c >= 51 && c <= 67) {
            description = "Lluvia";
            icon = "bi-cloud-drizzle";
        } else if (c >= 71 && c <= 77) {
            description = "Nieve";
            icon = "bi-cloud-snow";
        } else if (c >= 80 && c <= 82) {
            description = "Chaparrones";
            icon = "bi-cloud-rain-heavy";
        } else if (c >= 95) {
            description = "Tormenta";
            icon = "bi-cloud-lightning-rain";
        } else {
            description = "Nublado";
            icon = "bi-cloud";
        }

        return new WeatherDTO(description, icon,
                max == null ? 0 : (int) Math.round(max),
                min == null ? 0 : (int) Math.round(min),
                rain);
    }
}

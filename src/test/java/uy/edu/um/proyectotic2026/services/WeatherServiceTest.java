package uy.edu.um.proyectotic2026.services;

import org.junit.jupiter.api.Test;
import uy.edu.um.proyectotic2026.dto.WeatherDTO;
import uy.edu.um.proyectotic2026.entities.Locality;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class WeatherServiceTest {

    // Apunta a una direccion que no existe: simula que no hay internet
    private final WeatherService weatherService = new WeatherService("http://localhost:1");

    @Test
    void withoutConnectionReturnsEmptyAndDoesNotFail() {
        Locality locality = Locality.builder().name("Cabo Polonio").latitude(-34.4).longitude(-53.78).build();

        assertTrue(weatherService.getForecast(locality, List.of(LocalDate.now().plusDays(1))).isEmpty());
    }

    @Test
    void datesOutOfRangeAreNotRequested() {
        Locality locality = Locality.builder().name("Cabo Polonio").latitude(-34.4).longitude(-53.78).build();

        assertTrue(weatherService.getForecast(locality, List.of(LocalDate.now().plusDays(40))).isEmpty());
    }

    @Test
    void translatesWeatherCodes() {
        WeatherDTO sunny = weatherService.toWeather(0, 24.6, 15.2, 0);
        assertEquals("Despejado", sunny.getDescription());
        assertEquals(25, sunny.getMaxTemperature());
        assertEquals(15, sunny.getMinTemperature());

        assertEquals("Lluvia", weatherService.toWeather(61, 18.0, 12.0, 80).getDescription());
        assertEquals("Tormenta", weatherService.toWeather(95, 18.0, 12.0, 90).getDescription());
    }

    // El pronostico de un lugar se pide una vez y se guarda: abrir otra actividad del mismo lugar no espera a Open-Meteo
    @Test
    void forecastOfAPlaceIsRequestedOnlyOnce() {
        AtomicInteger requests = new AtomicInteger();
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        WeatherService service = new WeatherService("http://localhost:1") {
            @Override
            Map<LocalDate, WeatherDTO> fetchForecast(double latitude, double longitude, LocalDate from, LocalDate to) {
                requests.incrementAndGet();
                return Map.of(tomorrow, toWeather(0, 24.0, 15.0, 0));
            }
        };
        Locality polonio = Locality.builder().name("Cabo Polonio").latitude(-34.4).longitude(-53.78).build();
        Locality colonia = Locality.builder().name("Colonia").latitude(-34.47).longitude(-57.84).build();

        assertEquals("Despejado", service.getForecast(polonio, List.of(tomorrow)).get(tomorrow).getDescription());
        assertEquals("Despejado", service.getForecast(polonio, List.of(tomorrow)).get(tomorrow).getDescription());
        assertEquals(1, requests.get());

        service.getForecast(colonia, List.of(tomorrow));
        assertEquals(2, requests.get());
    }

    // Sin internet tampoco se reintenta en cada pagina: se espera unos minutos
    @Test
    void failedForecastIsNotRetriedOnEveryPage() {
        AtomicInteger requests = new AtomicInteger();
        WeatherService service = new WeatherService("http://localhost:1") {
            @Override
            Map<LocalDate, WeatherDTO> fetchForecast(double latitude, double longitude, LocalDate from, LocalDate to) {
                requests.incrementAndGet();
                return null;
            }
        };
        Locality polonio = Locality.builder().name("Cabo Polonio").latitude(-34.4).longitude(-53.78).build();

        assertTrue(service.getForecast(polonio, List.of(LocalDate.now().plusDays(1))).isEmpty());
        assertTrue(service.getForecast(polonio, List.of(LocalDate.now().plusDays(2))).isEmpty());
        assertEquals(1, requests.get());
    }
}

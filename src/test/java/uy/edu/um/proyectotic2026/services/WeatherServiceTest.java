package uy.edu.um.proyectotic2026.services;

import org.junit.jupiter.api.Test;
import uy.edu.um.proyectotic2026.dto.WeatherDTO;
import uy.edu.um.proyectotic2026.entities.Locality;

import java.time.LocalDate;
import java.util.List;

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
}

package uy.edu.um.proyectotic2026.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import uy.edu.um.proyectotic2026.dto.ActivitySearchDTO;
import uy.edu.um.proyectotic2026.entities.Activity;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.repository.ActivityRepository;
import uy.edu.um.proyectotic2026.repository.ActivityTypeRepository;
import uy.edu.um.proyectotic2026.repository.DepartmentRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Buscador del portal, con las actividades de ejemplo del DataLoader
@SpringBootTest
@Transactional
class PortalServiceTest {

    @Autowired
    private PortalService portalService;

    @Autowired
    private ActivityRepository activityRepo;

    @Autowired
    private DepartmentRepository departmentRepo;

    @Autowired
    private ActivityTypeRepository typeRepo;

    private List<String> names(List<Activity> activities) {
        return activities.stream().map(Activity::getName).toList();
    }

    private Long departmentId(String name) {
        return departmentRepo.findAll().stream().filter(d -> d.getName().equals(name)).findFirst().orElseThrow().getId();
    }

    @Test
    void withoutFiltersReturnsAllVisible() {
        assertEquals(7, portalService.searchActivities(new ActivitySearchDTO()).size());
    }

    @Test
    void filtersByLocality() {
        Activity activity = portalService.searchActivities(new ActivitySearchDTO()).get(0);
        ActivitySearchDTO search = new ActivitySearchDTO();
        search.setLocalityId(activity.getLocality().getId());

        List<Activity> result = portalService.searchActivities(search);

        assertTrue(names(result).contains(activity.getName()));
        assertTrue(result.stream().allMatch(a -> a.getLocality().getId().equals(activity.getLocality().getId())));
    }

    @Test
    void filtersByDepartment() {
        ActivitySearchDTO search = new ActivitySearchDTO();
        search.setDepartmentId(departmentId("Lavalleja"));

        List<String> result = names(portalService.searchActivities(search));

        assertEquals(List.of("Rappel en Villa Serrana", "Trekking al Salto del Penitente"), result);
    }

    @Test
    void filtersByType() {
        ActivitySearchDTO search = new ActivitySearchDTO();
        search.setTypeId(typeRepo.findByName("Actividad acuática").orElseThrow().getId());

        assertEquals(List.of("Kayak a la Isla Gorriti"), names(portalService.searchActivities(search)));
    }

    @Test
    void filtersByKeywordIgnoringCase() {
        ActivitySearchDTO search = new ActivitySearchDTO();
        search.setKeyword("  LOBOS ");

        assertEquals(List.of("Avistaje de lobos marinos"), names(portalService.searchActivities(search)));
    }

    @Test
    void filtersByMaxPrice() {
        ActivitySearchDTO search = new ActivitySearchDTO();
        search.setMaxPrice(new BigDecimal("900"));

        List<Activity> result = portalService.searchActivities(search);

        assertFalse(result.isEmpty());
        assertTrue(result.stream().allMatch(a -> a.getPrice().compareTo(new BigDecimal("900")) <= 0));
    }

    @Test
    void filtersByDate() {
        // El rappel tiene fechas dentro de 3, 6, 10 y 13 dias: dentro de 4 dias no hay
        ActivitySearchDTO search = new ActivitySearchDTO();
        search.setKeyword("rappel");
        search.setDate(LocalDate.now().plusDays(3));
        assertEquals(1, portalService.searchActivities(search).size());

        search.setDate(LocalDate.now().plusDays(4));
        assertTrue(portalService.searchActivities(search).isEmpty());
    }

    @Test
    void hiddenActivityIsNotShown() {
        Activity activity = activityRepo.findAll().get(0);
        activity.setPublished(false);
        activityRepo.save(activity);

        assertFalse(names(portalService.searchActivities(new ActivitySearchDTO())).contains(activity.getName()));
        assertThrows(EntityNotFoundException.class, () -> portalService.getPublicActivity(activity.getId()));
    }

    @Test
    void featuredAreSortedByNextDate() {
        List<Activity> featured = portalService.getFeaturedActivities(6);

        assertEquals(6, featured.size());
        for (int i = 1; i < featured.size(); i++)
            assertFalse(featured.get(i).getUpcomingInstances().get(0).getStartDateTime()
                    .isBefore(featured.get(i - 1).getUpcomingInstances().get(0).getStartDateTime()));
    }
}

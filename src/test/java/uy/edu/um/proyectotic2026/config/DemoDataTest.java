package uy.edu.um.proyectotic2026.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.util.ReflectionTestUtils;
import uy.edu.um.proyectotic2026.repository.DepartmentRepository;
import uy.edu.um.proyectotic2026.repository.LocalityRepository;
import uy.edu.um.proyectotic2026.repository.UserRepository;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Con una base que no se borra (Supabase), la app arranca muchas veces sobre los mismos datos:
// ni data.sql ni DataLoader pueden duplicar nada.
@SpringBootTest
class DemoDataTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DataLoader dataLoader;

    @Autowired
    private DepartmentRepository departmentRepo;

    @Autowired
    private LocalityRepository localityRepo;

    @Autowired
    private UserRepository userRepo;

    @Test
    void restartingDoesNotDuplicateData() {
        long departments = departmentRepo.count();
        long localities = localityRepo.count();
        long users = userRepo.count();

        // Lo mismo que pasa en un segundo arranque
        new ResourceDatabasePopulator(new ClassPathResource("data.sql")).execute(dataSource);
        dataLoader.run();

        assertEquals(19, departments);
        assertEquals(departments, departmentRepo.count());
        assertEquals(localities, localityRepo.count());
        assertEquals(users, userRepo.count());
    }

    @Test
    void emptyDemoPasswordStopsTheApp() {
        // DataLoader nuevo, fuera de Spring: falla antes de usar los repositorios
        DataLoader loader = new DataLoader();
        ReflectionTestUtils.setField(loader, "demoPassword", "");

        assertThrows(IllegalStateException.class, loader::run);
    }
}

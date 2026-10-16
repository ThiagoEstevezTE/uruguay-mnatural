package uy.edu.um.proyectotic2026.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.edu.um.proyectotic2026.dto.ActivitySearchDTO;
import uy.edu.um.proyectotic2026.dto.ActivitySummaryDTO;
import uy.edu.um.proyectotic2026.dto.BusinessResponseDTO;
import uy.edu.um.proyectotic2026.dto.BusinessVerificationDTO;
import uy.edu.um.proyectotic2026.entities.Business;
import uy.edu.um.proyectotic2026.entities.BusinessStatus;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.services.BusinessService;
import uy.edu.um.proyectotic2026.services.PortalService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

// API para que otras plataformas se integren con Uruguay Mnatural.
// Son las 5 operaciones que pide la letra. Documentacion en /swagger-ui.html
@RestController
@RequestMapping("/api/v1")
@Tag(name = "API pública", description = "Operaciones para integrar otras plataformas con Uruguay Mnatural")
public class PublicApiRestController {

    @Autowired
    private BusinessService businessService;

    @Autowired
    private PortalService portalService;

    @GetMapping("/businesses")
    @Operation(summary = "Listado de todos los emprendimientos aprobados")
    public List<BusinessResponseDTO> businesses() {
        return businessService.getApprovedBusinesses().stream()
                .map(BusinessResponseDTO::from)
                .toList();
    }

    @GetMapping("/businesses/{id}/verify")
    @Operation(summary = "Verificar emprendimiento", description = "Indica si el emprendimiento fue aprobado por Uruguay Mnatural")
    public ResponseEntity<?> verifyBusiness(@PathVariable("id") Long id) {
        try {
            Business business = businessService.getById(id);
            return ResponseEntity.ok(new BusinessVerificationDTO(business.getId(), business.getName(),
                    business.getStatus(), business.getStatus() == BusinessStatus.APPROVED));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/activities")
    @Operation(summary = "Actividades de un día", description = "Actividades con alguna fecha ese día, filtrando por departamento o localidad")
    public List<ActivitySummaryDTO> activitiesOnDate(
            @Parameter(description = "Día, formato AAAA-MM-DD", example = "2026-10-20")
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "localityId", required = false) Long localityId) {

        return portalService.getActivitiesOnDate(date, departmentId, localityId).stream()
                .map(a -> ActivitySummaryDTO.from(a, portalService.getAverage(a)))
                .toList();
    }

    @GetMapping("/activities/top")
    @Operation(summary = "Top 3 del mes por departamento", description = "Las 3 actividades mejor puntuadas con las valoraciones de ese mes")
    public List<ActivitySummaryDTO> topRated(
            @RequestParam("departmentId") Long departmentId,
            @Parameter(description = "Mes, formato AAAA-MM. Si no se indica, el mes actual", example = "2026-10")
            @RequestParam(value = "month", required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {

        YearMonth selectedMonth = month == null ? YearMonth.now() : month;
        return portalService.getTopRated(departmentId, selectedMonth).stream()
                .map(a -> ActivitySummaryDTO.from(a, portalService.getMonthAverage(a, selectedMonth)))
                .toList();
    }

    @GetMapping("/activities/search")
    @Operation(summary = "Buscar actividades por palabra clave")
    public List<ActivitySummaryDTO> search(@RequestParam("keyword") String keyword) {
        ActivitySearchDTO search = new ActivitySearchDTO();
        search.setKeyword(keyword);
        return portalService.searchActivities(search).stream()
                .map(a -> ActivitySummaryDTO.from(a, portalService.getAverage(a)))
                .toList();
    }
}

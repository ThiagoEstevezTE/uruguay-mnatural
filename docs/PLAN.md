# Plan: qué falta y cómo se pensaba seguir

Este es el plan **tal como estaba pensado** al 16/10/2026. No es obligatorio: Thiago (y el equipo) pueden cambiarlo. Cada cosa está marcada según qué tan definida estaba:

- 🟢 **Definido en detalle**: el código ya se había escrito y explicado.
- 🟡 **Idea general**: se sabía qué hacer, pero el código no se había diseñado.
- ⚪ **Futuro**: sale de las actas o de la letra, pero no se planificó.

| Paso | Qué | Estado |
|---|---|---|
| 1–5 | Configuración, entidades, repositorios, service | ✅ hechos (ver `HISTORIAL.md`) |
| **6** | **Controller REST + prueba con Postman** | ⏳ DTOs hechos, controller vacío |
| 7 | Formulario web de registro (Thymeleaf) | pendiente |
| 8 | Tests unitarios | pendiente |

---

## Paso 6: `ProviderRestController` 🟢

### 6.c El controller

Hay que completar `src/main/java/uy/edu/um/proyectotic2026/controllers/ProviderRestController.java`. Este es el código que se le había dado a Lorenzo. Claude verificó que compila con el código actual:

```java
package uy.edu.um.proyectotic2026.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uy.edu.um.proyectotic2026.dto.BusinessResponseDTO;
import uy.edu.um.proyectotic2026.dto.ProviderRegistrationDTO;
import uy.edu.um.proyectotic2026.dto.ProviderResponseDTO;
import uy.edu.um.proyectotic2026.entities.Provider;
import uy.edu.um.proyectotic2026.exceptions.EntityNotFoundException;
import uy.edu.um.proyectotic2026.exceptions.InvalidDataException;
import uy.edu.um.proyectotic2026.services.ProviderService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/provider")
public class ProviderRestController {

    @Autowired
    private ProviderService providerService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody ProviderRegistrationDTO dto) {
        try {
            Provider provider = providerService.registerProvider(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(ProviderResponseDTO.from(provider));
        } catch (InvalidDataException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/businesses")
    public ResponseEntity<?> getBusinesses(@PathVariable("id") Long id) {
        try {
            List<BusinessResponseDTO> businesses = providerService.getBusinessesByProvider(id).stream()
                    .map(BusinessResponseDTO::from)
                    .toList();
            return ResponseEntity.ok(businesses);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
```

Cosas que se le explicaron a Lorenzo sobre este código:
- **`@RequestBody`** convierte el JSON en un `ProviderRegistrationDTO`.
- **`ResponseEntity<?>`**: el `?` es porque devuelve un DTO si sale bien y un `String` con el mensaje si sale mal.
- **Códigos HTTP:** `201 Created` en el registro, `400` para datos inválidos o duplicados, `404` si algo no existe.

### 6.d Pruebas con Postman

Configuración del request:
- **Método:** `POST`
- **URL:** `http://localhost:8080/api/v1/provider/register`
- **Body:** raw → JSON

```json
{
  "email": "Ana@Mail.com",
  "password": "secreta123",
  "name": "Ana Perez",
  "document": "12345678",
  "phone": "099123456",
  "type": "INDIVIDUAL",
  "businessName": "Cabalgatas del Polonio",
  "businessDescription": "Cabalgatas por la costa rochense",
  "businessAddress": "Ruta 10 km 264",
  "businessPhone": "099123456",
  "businessEmail": "contacto@polonio.uy",
  "localityId": 22
}
```

**Respuesta esperada:** `201`, con el email en minúsculas, sin contraseña y con `businesses[0].status = "PENDING"`, `locality = "Cabo Polonio"` y `department = "Rocha"`.

Casos de error a probar:

| Prueba | Qué cambiar | Resultado esperado |
|---|---|---|
| Duplicado | Mandar lo mismo otra vez | `400` "Ya existe un usuario registrado con el email..." |
| Contraseña corta | `"password": "123"` y otro email | `400` "La contraseña debe tener al menos 8 caracteres" |
| Localidad inexistente | `"localityId": 999`, con otro email y otro documento | `404` "Localidad no encontrada" |
| Listar emprendimientos | `GET http://localhost:8080/api/v1/provider/1/businesses` | `200` con la lista |

Si `"type"` no es exactamente `"INDIVIDUAL"` o `"COMPANY"`, Spring devuelve `400` antes de llegar al código.

**Verificación previa (16/10/2026).** Claude armó una copia aparte del proyecto, le agregó exactamente este controller y corrió estas pruebas con `curl`. Todas dieron el resultado esperado:
- registro → `201` con el JSON correcto (email en minúsculas, sin contraseña, `PENDING`, Cabo Polonio / Rocha);
- duplicado → `400`;
- contraseña corta → `400`;
- localidad 999 → `404`;
- listar → `200`;
- `"type": "individual"` en minúsculas → `400`.

O sea: el código actual de entidades, repositorios y service funciona. **En el proyecto real el controller sigue vacío**, y escribirlo y probarlo con Postman es justamente el próximo paso.

---

## Paso 7: Formulario web con Thymeleaf 🟡

**Antes de empezar:** confirmar con Thiago que el equipo usa Thymeleaf y no React (ver "Decisiones abiertas" en `HISTORIAL.md`).

**Lo único que se había dicho** es que habría un formulario de registro con un desplegable "¿Sos empresa o persona particular?" para el campo `type`, y que el DTO tiene getters y setters para que Thymeleaf lo llene. **El código no se diseñó.**

**Sugerencia técnica de Claude** (no es una decisión tomada; revisarla con Thiago):
- **Controller:**
  - Un `@Controller` web aparte (por ejemplo `ProviderWebController`), como `FuncionarioWebController` / `SingerController` del ejemplo.
  - Un `@GetMapping` que muestre el formulario.
  - Un `@PostMapping` que reciba el `ProviderRegistrationDTO` con `@ModelAttribute` y llame a `providerService.registerProvider`.
- **Por qué un `@PostMapping` propio en el controller web:** el `alta.html` del ejemplo hace `POST` directo a un endpoint REST con parámetros de formulario, pero `ProviderRestController.register` espera **JSON** (`@RequestBody`). Por eso un `<form>` HTML común no puede apuntar directo ahí.
- **Plantilla:**
  - Usar `th:object` / `th:field` sobre el DTO.
  - Desplegables de departamento y localidad cargados con `DepartmentRepository.findAllByOrderByNameAsc()` y `LocalityRepository.findByDepartmentIdOrderByNameAsc(...)`.
- **Errores:** mostrar el mensaje de `InvalidDataException` / `EntityNotFoundException` en la misma página, con `model.addAttribute("mensaje", ...)`, como hace `singerdetail.html`.

---

## Paso 8: Tests unitarios 🟡

**Lo que pide la letra:** "Tests unitarios: cobertura de los principales casos de uso".

**Lo que se había pensado:**
- **Tests de `ProviderService` con Mockito** (mockeando los repositorios). Casos:
  - registro exitoso: contraseña cifrada, email en minúsculas, `role = PROVIDER`, emprendimiento `PENDING`;
  - email duplicado;
  - documento duplicado;
  - localidad inexistente;
  - datos obligatorios faltantes;
  - contraseña corta.
- **Opcional: tests del controller** con `@WebMvcTest` + `@MockitoBean`.

Para los paquetes de las anotaciones de test en Spring Boot 4, ver `HISTORIAL.md` sección 2.

**Detalle a tener en cuenta:** `ProviderService` crea el `BCryptPasswordEncoder` con `new` dentro de la clase (no es un bean inyectado). En un test con Mockito se usa el encoder real. Se puede verificar que la contraseña guardada empiece con `$2`.

---

## Después del registro ⚪

Salen de las actas y de la letra. No hay diseño hecho.

### Lo inmediato para cerrar el registro

- **Bandeja de solicitudes del administrador** (Acta N.º 1, 4.2: "el sistema deberá contar con una bandeja de solicitudes desde la cual el administrador pueda aprobar o rechazar cada emprendimiento"):
  - Crear la entidad `Administrator extends User` (en el MER tiene `nombre`).
  - Listar los emprendimientos pendientes con `businessRepo.findByStatus(BusinessStatus.PENDING)`.
  - **Aprobar:** `status = APPROVED`, guardar `resolutionDate`.
  - **Rechazar:** `status = REJECTED`, guardar `rejectionReason` y `resolutionDate`.
  - Agregar a `Business` la relación con el administrador que resolvió (`admin_resolutor_id` en el modelo relacional).
- **Regla de negocio asociada:** solo un emprendimiento `APPROVED` puede publicar actividades y ser visible en el portal público (Acta 4.2).

### Otras cosas pendientes del proyecto

- **Login y autenticación** (las contraseñas ya se guardan con BCrypt).
- **Migrar de H2 a PostgreSQL o MySQL** (lo exige la letra).
- **El resto del MER:** turistas, actividades, instancias, reservas, pagos, valoraciones, etc.

### Puntos que las actas dejan abiertos con el cliente

- **Cupos:** el acta dice que las actividades no manejan cupos, pero aclara que se volverá a consultar. Por eso el MER tiene `cupo_maximo`.
- **Cancelaciones del prestador:** los motivos y el porcentaje de devolución de cada uno quedan pendientes de definir con el cliente.

# Historial del desarrollo: registro de prestadores

Lo escribió Claude (en la sesión de Lorenzo Gianola) el 16/10/2026, a partir del trabajo hecho entre el 28/09 y el 16/10/2026.

---

## 1. Cómo se trabajó

- **Primer intento descartado.** Al principio, Claude escribió una versión completa del registro de prestadores por su cuenta. Lorenzo pidió **revertirla por completo**: quería aprender haciendo, no recibir el código hecho. **Nada de esa versión quedó en el proyecto.**
- **Ejemplos de Moodle como guía.** Después, Lorenzo compartió los ejemplos que los docentes publicaron en Moodle (`demospring`, `demospring_post`, `demospring_withform`, `demospring_thymeleaf`). El más completo, `demospring_thymeleaf`, está en `../ejemplos-moodle/` y es la **guía de estilo** del proyecto.
- **Modo de trabajo.** A partir de ahí se trabajó **paso a paso**:
  1. Claude explicaba la idea y daba el código en el chat.
  2. Lorenzo creaba los archivos en IntelliJ.
  3. Claude revisaba los archivos y señalaba errores.

  Todo el código actual lo escribió Lorenzo siguiendo esas indicaciones.

---

## 2. Stack técnico (según `pom.xml`)

| Cosa | Versión / detalle |
|---|---|
| Spring Boot | **4.1.1** (Spring Framework 7, Hibernate 7, Jackson 3) |
| Java | **25** |
| Build | Maven, con Maven Wrapper incluido |
| Base de datos | **H2 en memoria** por ahora (la letra exige PostgreSQL o MySQL al final; ver `PLAN.md`) |
| Web | `spring-boot-starter-webmvc` (REST) + `spring-boot-starter-thymeleaf` (vistas) |
| Otras | Lombok, `spring-boot-devtools`, `spring-boot-h2console`, `spring-security-crypto` (**solo** para cifrar contraseñas con BCrypt; no activa login ni seguridad web) |
| Tests | `spring-boot-starter-data-jpa-test`, `spring-boot-starter-webmvc-test` (traídos por el generador del proyecto) |

### Diferencias con los ejemplos de Moodle

Los ejemplos usan Spring Boot 3.3 y Java 17. Hay dos diferencias que importan:

- **No hay `JpaConfiguration`.** Los ejemplos tienen una clase `JpaConfiguration` que arma a mano el DataSource y lee `schema.sql`. En este proyecto **no se copió**: en Boot 4 todo eso se configura en `application.properties`, y las tablas las crea Hibernate a partir de las entidades.
- **Tests en Spring Boot 4.** Algunas anotaciones cambiaron de paquete respecto de Boot 3. Lo verificó Claude mirando los jars de 4.1.1:
  - `@WebMvcTest` y `@AutoConfigureMockMvc` → `org.springframework.boot.webmvc.test.autoconfigure`
  - `@DataJpaTest` → `org.springframework.boot.data.jpa.test.autoconfigure`
  - `@MockBean` ya no existe; se usa `@MockitoBean` (`org.springframework.test.context.bean.override.mockito.MockitoBean`).

---

## 3. Arquitectura (MVC en capas, como pide la letra)

```
uy.edu.um.proyectotic2026
├── entities      → modelo JPA (según el MER)
├── repository    → interfaces JpaRepository
├── services      → lógica de negocio y validaciones
├── controllers   → entrada HTTP (REST con @RestController; vistas con @Controller)
├── dto           → objetos de entrada y salida (no son entidades)
└── exceptions    → excepciones propias (checked, como en el ejemplo del curso)
```

Las convenciones copiadas del ejemplo de Moodle son:
- los nombres de los paquetes;
- Lombok en las entidades (`@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`);
- inyección con `@Autowired` en campos;
- excepciones propias `InvalidDataException` y `EntityNotFoundException` (checked, `extends Exception`);
- controllers REST bajo `/api/v1/...` que devuelven `ResponseEntity` y traducen las excepciones con `try/catch`.

---

## 4. Lo que se hizo, paso a paso

### Paso 1: Configuración ✅

- Se agregó `spring-boot-starter-thymeleaf` al `pom.xml`.
- `application.properties` quedó así:
  - **Base de datos:** H2 en memoria (`jdbc:h2:mem:uymnatural`, usuario `sa`, sin contraseña).
  - **Consola H2:** habilitada en `/h2-console`.
  - **Tablas:** `ddl-auto=create-drop`, así que Hibernate crea las tablas al arrancar y las borra al apagar.
  - **Log:** `show-sql=true`, para ver en la consola el SQL que se ejecuta.
  - **Datos iniciales:** `spring.jpa.defer-datasource-initialization=true`, para que `data.sql` corra **después** de que Hibernate cree las tablas.

### Paso 2: `User` y `Provider` (herencia) ✅

En el MER, USUARIO se especializa en PRESTADOR, TURISTA y ADMINISTRADOR. Se implementó con **herencia JPA `JOINED`**:

- **`User`**
  - Es **abstracta**: nunca existe un usuario "a secas".
  - Tabla `users`. No se llama `user` porque `USER` es palabra reservada en SQL.
  - Campos: `id`, `email` (único), `password`, `role` (enum `Role`: `TOURIST`, `PROVIDER`, `ADMIN`) y `active` (default `true`).
- **`Provider extends User`**
  - Tabla `providers`, con `@PrimaryKeyJoinColumn(name = "user_id")`: la PK es también FK a `users`, igual que `usuario_id PK,FK` en el modelo relacional.
  - Campos: `name`, `document` (único), `phone` y `type` (enum `ProviderType`: `COMPANY` / `INDIVIDUAL`, porque la letra dice que pueden ser empresas o personas particulares).
- **Anotaciones de Lombok y JPA usadas:**
  - `@SuperBuilder` en las dos clases, en vez de `@Builder`: el `@Builder` del ejemplo no funciona con herencia.
  - `@Builder.Default` en los campos con valor por defecto; si no, el builder los ignora.
  - `@Enumerated(EnumType.STRING)` en todos los enums, para que se guarde el texto y no el número.

### Paso 3: Ubicación y emprendimiento ✅

- **`Department`** (tabla `departments`): `id`, `name` (único).
- **`Locality`** (tabla `localities`): `id`, `name`, `@ManyToOne department` (FK `department_id`).
- **`Business`** (tabla `businesses`), que es el EMPRENDIMIENTO del MER:
  - **Relaciones:** `@ManyToOne provider` (FK `provider_id`) y `@ManyToOne locality` (FK `locality_id`).
  - **Datos:** `name`, `description` (largo 2000), `address`, `phone`, `email`.
  - **Estado:** `status` (enum `BusinessStatus`: `PENDING` / `APPROVED` / `REJECTED`, default **`PENDING`** con `@Builder.Default`).
  - **Fechas y resolución:** `requestDate` (`LocalDateTime`, obligatorio), `resolutionDate`, `rejectionReason`.
- **En `Provider`** se agregaron:
  - `@OneToMany(mappedBy = "provider", cascade = CascadeType.ALL) List<Business> businesses`;
  - el método `addBusiness(Business)`, que conecta los dos lados de la relación. Es el mismo patrón que `Singer.addAlbum()` en el ejemplo.
- **`src/main/resources/data.sql`** carga los **19 departamentos** y **29 localidades**. Los `department_id` están escritos a mano según el orden de inserción, igual que el `test-data.sql` del ejemplo. La localidad de id `22` es Cabo Polonio (Rocha).

### Paso 4: Repositorios ✅

| Repositorio | Métodos extra (Spring genera el SQL por el nombre) |
|---|---|
| `UserRepository` | `boolean existsByEmail(String)`: el email se valida contra **todos** los usuarios, no solo prestadores, para que el login futuro no sea ambiguo. |
| `ProviderRepository` | `boolean existsByDocument(String)` |
| `BusinessRepository` | `List<Business> findByProviderId(Long)` y `List<Business> findByStatus(BusinessStatus)`. El segundo **todavía no se usa**; está pensado para la bandeja del administrador. |
| `LocalityRepository` | `List<Locality> findByDepartmentIdOrderByNameAsc(Long)`: todavía no se usa; está pensado para el formulario. |
| `DepartmentRepository` | `List<Department> findAllByOrderByNameAsc()`: todavía no se usa; está pensado para el formulario. |

### Paso 5: Excepciones, DTO de entrada y service ✅ (con una salvedad)

- **`exceptions/`:** `InvalidDataException` y `EntityNotFoundException`, idénticas al ejemplo.
- **`dto/ProviderRegistrationDTO`**
  - Es un objeto plano con `@Getter @Setter @NoArgsConstructor`.
  - **Datos del prestador:** `email`, `password`, `name`, `document`, `phone`, `type`.
  - **Datos del emprendimiento:** `businessName`, `businessDescription`, `businessAddress`, `businessPhone`, `businessEmail`, `localityId`.
  - **Por qué existe:** el registro trae datos de dos entidades a la vez, y así nadie puede mandar `role` o `status` desde afuera.
  - **Por qué tiene getters, setters y constructor vacío:** para que Thymeleaf pueda llenarlo desde un formulario (Paso 7).
- **`services/ProviderService`**
  - **`registerProvider(dto)`**, con `@Transactional`, hace en orden:
    1. `validate(dto)`: campos obligatorios, formato de email (regex simple) y contraseña de al menos 8 caracteres. Si algo falla, lanza `InvalidDataException`.
    2. Normaliza el email a minúsculas y le hace `trim` al documento.
    3. Controla duplicados con `existsByEmail` y `existsByDocument`. Si hay uno, lanza `InvalidDataException`.
    4. Busca la localidad. Si no existe, lanza `EntityNotFoundException`.
    5. Arma el `Provider` con `Provider.builder()`, `role = PROVIDER` y la contraseña cifrada con `BCryptPasswordEncoder`.
    6. Arma el `Business` con `requestDate = now()` y status `PENDING` (por default).
    7. Llama a `provider.addBusiness(business)` y a `providerRepo.save(provider)`. El cascade guarda también el `Business`.
  - **`getBusinessesByProvider(providerId)`**: si el prestador no existe, lanza `EntityNotFoundException`; si existe, devuelve `businessRepo.findByProviderId(...)`.
- **Salvedad:** el paso tenía una prueba manual (un `CommandLineRunner` temporal que llamaba al service al arrancar). **Lorenzo eligió no hacerla.**
  - Al armar este paquete, Claude probó el service **en una copia aparte** del proyecto, con el controller de `PLAN.md` agregado. Los 6 casos de prueba de `PLAN.md` dieron el resultado esperado (ver `PLAN.md`, Paso 6).
  - En el proyecto real, `registerProvider` todavía no se ejecutó, porque el controller sigue vacío.

### Paso 6: DTOs de salida y controller REST ⏳ (a medias)

- ✅ **`dto/BusinessResponseDTO`**
  - Campos: `id`, `name`, `description`, `address`, `locality` (nombre), `department` (nombre), `status`, `requestDate`.
  - Tiene un método `static from(Business)`.
- ✅ **`dto/ProviderResponseDTO`**
  - Campos: `id`, `email`, `name`, `document`, `phone`, `type` y `List<BusinessResponseDTO> businesses`.
  - Tiene un método `static from(Provider)`.
  - **Por qué existen los DTOs de salida:**
    - si se devolviera la entidad `Provider`, se expondría el hash de la contraseña;
    - y habría recursión infinita al convertir a JSON (`Provider` → `businesses` → `provider` → …).
- ❌ **`controllers/ProviderRestController`**: **el archivo existe pero está vacío** (solo tiene `package` y `public class ProviderRestController {}`). El código que se iba a escribir está en `PLAN.md`.

---

## 5. Equivalencia MER (español) → código (inglés)

La letra exige nombres en inglés, así que el MER y el código usan nombres distintos para lo mismo.

| MER / modelo relacional | Clase | Tabla | Campos (MER → código) |
|---|---|---|---|
| USUARIO | `User` (abstracta) | `users` | id, email, password, rol → `role`, activo → `active` |
| PRESTADOR | `Provider` | `providers` | usuario_id → `user_id`, nombre → `name`, documento → `document`, telefono → `phone`, tipo → `type` |
| DEPARTAMENTO | `Department` | `departments` | id, nombre → `name` |
| LOCALIDAD | `Locality` | `localities` | id, departamento_id → `department_id`, nombre → `name` |
| EMPRENDIMIENTO | `Business` | `businesses` | ver el detalle abajo |

Detalle de EMPRENDIMIENTO → `Business`:
- **Claves:** id; prestador_id → `provider_id`; localidad_id → `locality_id`.
- **Datos:** nombre → `name`, descripcion → `description`, direccion → `address`, telefono → `phone`, email.
- **Estado y resolución:** estado → `status`, fecha_solicitud → `requestDate`, fecha_resolucion → `resolutionDate`, motivo_rechazo → `rejectionReason`.
- **Pendiente:** `admin_resolutor_id` **todavía no existe en el código**. Se agrega cuando se cree la entidad `Administrator` (ver `PLAN.md`).

**Todavía no implementadas:** TURISTA, ADMINISTRADOR, ACTIVIDAD, INSTANCIA_ACTIVIDAD, TIPO_ACTIVIDAD, EQUIPAMIENTO, IMAGEN_ACTIVIDAD, RESERVA, PAGO, MEDIO_PAGO, VALORACION, REEMBOLSO y MOTIVO_CANCELACION.

---

## 6. Estado actual (verificado el 16/10/2026)

- `mvnw compile` → OK.
- `mvnw test` → 1 test (`contextLoads`), pasa.

### Detalles menores conocidos (no rompen nada)

- **En `registerProvider`, `businessAddress` no tiene `.trim()`**, a diferencia de los otros campos.
- **Un comentario de `application.properties` tiene un carácter roto.** El comentario dice "DESPUÉS" y la "É" se ve mal, porque IntelliJ guarda los `.properties` en ISO-8859-1. Es solo un comentario. Conviene escribir los comentarios de ese archivo sin tildes.

### Decisiones abiertas (no las decidió nadie todavía)

- **Frontend: Thymeleaf o React.** Se agregó Thymeleaf porque es lo que muestran los ejemplos de Moodle, y Claude le preguntó a Lorenzo si el equipo ya lo había decidido. **No hubo confirmación del equipo.**
- **Base de datos final:** PostgreSQL o MySQL. La letra permite cualquiera de las dos.
- **Repositorio Git del equipo:** desconocido.


---

## 7. Continuación (Thiago, desde el 16/10/2026)

El proyecto pasó a la Mac de Thiago, en `~/ProyectoTIC`, con repositorio Git local. Se avanzó en tandas, cada una con sus tests y su commit (ver `git log`). **El detalle de todo lo nuevo está en `docs/GUIA_DEL_CODIGO.md`.**

### Decisiones que estaban abiertas

| Decisión | Qué se eligió | Por qué |
|---|---|---|
| Frontend | **Thymeleaf** + Bootstrap (servido desde el proyecto) | Es lo de los ejemplos del curso; queda una sola app y no hace falta un segundo proyecto. |
| Base de datos | **PostgreSQL**: Supabase (cuenta del equipo) o un Postgres local | Lo permite la letra. Supabase es PostgreSQL con un panel web, y es más fácil de compartir con el equipo. Los tests usan H2. |
| Repositorio | Git local; después se sube al repositorio privado del equipo | — |

### Cambios a lo que había

- `ProviderRestController`: se completó con el código del Paso 6 de `PLAN.md`, y se agregaron sus tests.
- `ProviderService`: `businessAddress` ahora tiene `.trim()` (era uno de los detalles menores), y se agregó `getByEmail`.
- `spring-security-crypto` se reemplazó por `spring-boot-starter-security`, que la incluye y además da el login.
- `data.sql`: las localidades ahora tienen latitud y longitud, para el pronóstico del clima.
- `application.properties`: PostgreSQL con los datos de conexión en `.env`. Ver el README.
- Plazo para pagar (16/10): la reserva pendiente vence a las 2 horas o al cierre de reservas de la fecha, lo que pase antes. El acta habla de una vigencia **máxima** de 2 horas (punto 4.8), así que entra en la letra; queda anotado en `PLAN.md` para confirmarlo con el cliente.

### Lo que se agregó

- Login con roles (turista, prestador y administrador).
- Registro de turistas y prestadores desde formularios.
- Bandeja del administrador para aprobar o rechazar emprendimientos.
- Actividades, fechas, tipos y equipamiento (con su ABM).
- Portal público con buscador.
- Reservas y pagos con un servicio externo simulado.
- QR y registro de asistencia.
- Valoraciones.
- API pública de la letra, documentada con Swagger.
- Pronóstico del clima.

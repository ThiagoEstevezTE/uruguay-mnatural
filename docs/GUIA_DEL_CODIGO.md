# Guía del código: Uruguay Mnatural

Explica qué hace cada parte del sistema y por qué está hecha así, sin ir línea por línea.

- **Parte 1:** los conceptos que aparecen en todo el proyecto. Conviene saberlos de memoria.
- **Parte 2:** el recorrido por el código, sección por sección.

Lo marcado con ⭐ es probable que lo pregunten en la defensa.

---

# Parte 1: conceptos clave

## 1.1 La arquitectura: MVC en capas ⭐

La letra pide MVC en capas. Cada pedido del navegador recorre siempre el mismo camino:

```
Navegador → Controller → Service → Repository → Base de datos
                ↓
              Vista (HTML con Thymeleaf)
```

| Capa | Paquete | Qué hace | Qué NO hace |
|---|---|---|---|
| **Controller** | `controllers` | Recibe el pedido HTTP, llama al service y elige qué página mostrar (o qué JSON devolver). | No tiene reglas de negocio. |
| **Service** | `services` | Las reglas del negocio: validar, calcular, decidir. | No sabe nada de HTML ni de HTTP. |
| **Repository** | `repository` | Leer y guardar en la base. | No toma decisiones. |
| **Entity (Model)** | `entities` | Las clases que se guardan en la base (una tabla cada una). | — |
| **View** | `templates/*.html` | Lo que ve el usuario. | No accede a la base. |
| **DTO** | `dto` | Objetos para entrar o sacar datos sin exponer las entidades. | — |

**Por qué así:** cada capa se puede cambiar o probar sin tocar las otras. Por ejemplo, la misma lógica de `ProviderService` la usan el formulario web y la API REST.

## 1.2 Anotaciones de Spring que aparecen en todos lados ⭐

| Anotación | Dónde | Qué significa |
|---|---|---|
| `@Entity`, `@Table(name = "...")` | entidades | Esta clase es una tabla de la base. |
| `@Id`, `@GeneratedValue(strategy = IDENTITY)` | entidades | Clave primaria que la base numera sola. |
| `@Column(nullable = false, unique = true)` | entidades | Restricciones de la columna (NOT NULL, UNIQUE). |
| `@ManyToOne` + `@JoinColumn` | entidades | Clave foránea: muchas de estas apuntan a una de aquella. |
| `@OneToMany(mappedBy = ...)` | entidades | El otro lado de un `@ManyToOne`. No crea columna; la FK está del otro lado. |
| `@Enumerated(EnumType.STRING)` | entidades | Guarda el enum como texto (`"PENDING"`), no como número. |
| `@Service` | services | Spring crea un objeto de esta clase y lo comparte. |
| `@Autowired` | campos | "Spring, pasame el objeto que creaste". Es la **inyección de dependencias**. |
| `@Transactional` | métodos del service | Todo lo que hace el método se guarda junto o no se guarda nada. |
| `@Controller` | controllers web | Devuelve el nombre de una página HTML. |
| `@RestController` | controllers de la API | Devuelve datos (JSON), no páginas. |
| `@GetMapping`, `@PostMapping` | métodos del controller | Qué URL y qué método HTTP atiende. |
| `@PathVariable` | parámetros | Un valor que viene en la URL (`/activities/{id}`). |
| `@RequestParam` | parámetros | Un valor que viene en `?clave=valor` o de un campo de formulario suelto. |
| `@ModelAttribute` | parámetros | Arma un objeto (un DTO) con todos los campos de un formulario. |
| `@RequestBody` | parámetros | Arma un objeto a partir del JSON del pedido (API REST). |

## 1.3 Lombok

Lombok escribe el código repetitivo en lugar nuestro, al compilar:

- `@Getter` y `@Setter`: todos los getters y setters.
- `@NoArgsConstructor` y `@AllArgsConstructor`: los constructores.
- `@Builder`: permite crear objetos así, `Business.builder().name("...").build()`, sin un constructor de 15 parámetros.
- `@SuperBuilder`: lo mismo, pero funciona con herencia (`Provider extends User`).
- `@Builder.Default`: el valor por defecto del campo se respeta cuando se usa el builder.

## 1.4 Herencia de usuarios (JOINED) ⭐

En el MER, USUARIO se especializa en TURISTA, PRESTADOR y ADMINISTRADOR. En el código:

- `User` es **abstracta** y tiene lo común: email, contraseña, rol y activo.
- `Tourist`, `Provider` y `Administrator` heredan de `User`.
- Con `@Inheritance(strategy = JOINED)` hay una tabla `users` y una tabla por cada hijo. La PK del hijo es también FK a `users`, igual que en el modelo relacional.

**Por qué JOINED y no una sola tabla:** cada tipo tiene datos distintos (el turista tiene fecha de nacimiento y el prestador no). Con una sola tabla habría muchas columnas vacías y no se podría poner `NOT NULL`.

## 1.5 Excepciones propias (checked) ⭐

- `InvalidDataException`: los datos están mal (falta algo, está repetido, no cumple una regla).
- `EntityNotFoundException`: se buscó algo que no existe.

Las tira el **service** y las atrapa el **controller** con `try/catch`. El controller decide qué hacer: mostrar el mensaje en la página o devolver un código HTTP (400, 404). Es el mismo patrón del ejemplo de Moodle.

## 1.6 Optional y `orElseThrow`

`repo.findById(id)` no devuelve el objeto directo: devuelve un `Optional`, una caja que puede estar vacía si no existe. Se usa así:

```java
Locality locality = localityRepo.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Localidad no encontrada"));
```

Se lee: "dame la localidad y, si no existe, tirá esta excepción".

## 1.7 Repositorios: métodos que Spring escribe solo ⭐

Los repositorios son interfaces que extienden `JpaRepository<Entidad, TipoDeId>`. Así ya traen `save`, `findById`, `findAll`, `delete` y `count`.

Además, Spring arma la consulta SQL a partir del **nombre del método**:

| Método | SQL que genera Spring |
|---|---|
| `existsByEmail(email)` | `SELECT count(*) > 0 FROM users WHERE email = ?` |
| `findByStatus(status)` | `SELECT * FROM businesses WHERE status = ?` |
| `findByDepartmentIdOrderByNameAsc(id)` | `... WHERE department_id = ? ORDER BY name ASC` |

## 1.8 Streams

Aparecen para transformar listas:

```java
businesses.stream()
        .map(BusinessResponseDTO::from)   // cada Business se convierte en su DTO
        .toList();                        // se junta todo en una lista nueva
```

- `filter(...)`: se queda solo con los que cumplen la condición.
- `map(...)`: transforma cada elemento.
- `toList()`: junta el resultado en una lista.

## 1.9 Thymeleaf

Thymeleaf completa el HTML con los datos que el controller pone en el `Model`:

| Atributo | Para qué |
|---|---|
| `th:text="${activity.name}"` | Escribe un dato. |
| `th:each="a : ${activities}"` | Repite el elemento por cada uno de la lista. |
| `th:if="${error}"` | Muestra el elemento solo si se cumple la condición. |
| `th:href="@{/activities/{id}(id=${a.id})}"` | Arma un link con parámetros. |
| `th:action`, `th:object`, `th:field` | Formularios conectados a un DTO. |
| `th:replace="~{fragments/layout :: navbar}"` | Pega una parte común (el menú, el pie). |
| `sec:authorize="hasRole('ADMIN')"` | Muestra el elemento solo a ese rol. |

## 1.10 Seguridad: login y roles ⭐

- La configuración está en `config/SecurityConfig`. Ahí se define qué URLs puede ver cada rol:
  - `/admin/**` es solo para ADMIN;
  - `/provider/**`, solo para PROVIDER;
  - `/tourist/**`, solo para TOURIST;
  - el resto es público.
- **Contraseñas:** nunca se guardan en texto. Se guardan cifradas con **BCrypt**, que es un hash de una sola vía. Al iniciar sesión, BCrypt compara lo que escribiste con el hash guardado, sin "descifrarlo".
- **`LoginService`** es lo que usa Spring Security para buscar al usuario por email y saber su rol.
- **CSRF:** Spring agrega a cada formulario un token oculto, para que otro sitio no pueda mandar formularios en tu nombre. Thymeleaf lo pone solo con `th:action`. La API REST no lo usa, porque la llaman otros sistemas.

## 1.11 Tests

- **Tests unitarios con Mockito** (`@ExtendWith(MockitoExtension.class)`):
  - prueban un service solo, con los repositorios "de mentira" (`@Mock`);
  - `when(...).thenReturn(...)` dice qué devuelve el repositorio falso;
  - `@InjectMocks` crea el service con esos repositorios falsos adentro.
- **Tests de integración** (`@SpringBootTest` + `MockMvc`):
  - levantan la app entera con una base H2 en memoria;
  - simulan pedidos HTTP sin abrir un navegador.
- Se corren con `./mvnw test`.

---

# Parte 2: el código, sección por sección

## 2.1 Configuración y base de datos

**Archivos:** `pom.xml`, `application.properties`, `.env.example` y `src/test/resources/application.properties`.

- **Base:** PostgreSQL. Puede ser Supabase o un Postgres local; para el código es lo mismo, solo cambia la dirección.
- **Datos de conexión en `.env`:**
  - `application.properties` los lee de ahí con `spring.config.import=optional:file:.env[.properties]`;
  - `.env` está en `.gitignore`, así que las contraseñas no se suben a GitHub;
  - cada integrante crea su `.env` a partir de `.env.example`.
- **`ddl-auto=create`:** Hibernate crea las tablas a partir de las entidades cada vez que arranca. Después corren `data.sql` (departamentos y localidades) y `DataLoader` (usuarios de ejemplo).
- **Tests:** usan H2 en memoria (`src/test/resources/application.properties`), así no dependen de tener una base andando.
- **Bootstrap e iconos:** vienen como dependencias de Maven (webjars), así la página se ve bien aunque no haya internet en la demo.

## 2.2 Usuarios: turista, prestador y administrador

**Archivos:** `entities/User`, `Tourist`, `Provider`, `Administrator` y `Role`.

- Siguen el MER: USUARIO con sus tres especializaciones (ver 1.4).
- **Turista:** nombre, apellido, documento y fecha de nacimiento, que es lo acordado en el Acta 1, punto 4.1. La fecha de nacimiento sirve para controlar la edad mínima de cada actividad.
- **Administrador:** nombre.
- **`Business.resolvedBy`:** es el `admin_resolutor_id` del modelo relacional, el administrador que aprobó o rechazó el emprendimiento.

## 2.3 Login y permisos

**Archivos:** `config/SecurityConfig`, `services/LoginService` y `controllers/LoginController`.

- **`SecurityConfig`:**
  - define los permisos por URL (ver 1.10);
  - usa la pantalla de login propia (`/login`);
  - al salir, vuelve al inicio.
- **`LoginService.loadUserByUsername`:**
  - busca el usuario por email, en minúsculas y sin espacios;
  - le dice a Spring Security su contraseña cifrada y su rol;
  - un usuario con `active = false` no puede entrar.
- **`LoginController`:**
  - muestra el login;
  - tiene `/panel`, a donde va todo el mundo después de entrar. Desde ahí redirige: el administrador va a `/admin`, el prestador a `/provider` y el turista al inicio.

## 2.4 Datos de ejemplo

**Archivo:** `config/DataLoader`.

- Es un `CommandLineRunner`: Spring lo ejecuta una vez, apenas termina de arrancar.
- Crea las cuentas de prueba del README: un administrador, una turista, un prestador aprobado y uno pendiente.
- Si ya hay usuarios en la base, no hace nada. Así no se duplica nada si algún día se cambia `ddl-auto`.

## 2.5 API REST del registro de prestadores

**Archivo:** `controllers/ProviderRestController`.

- Es el código que había quedado diseñado en `docs/PLAN.md`, paso 6.
- `POST /api/v1/provider/register` recibe un JSON con los datos del prestador y de su emprendimiento:
  - devuelve **201** si se creó;
  - **400** si los datos están mal o repetidos;
  - **404** si la localidad no existe.
- `GET /api/v1/provider/{id}/businesses` devuelve los emprendimientos de un prestador.
- **Devuelve DTOs, no entidades** ⭐. Si devolviera la entidad `Provider`, se vería el hash de la contraseña. Además habría un bucle infinito al pasarlo a JSON, porque el prestador tiene emprendimientos y cada emprendimiento apunta a su prestador.

## 2.6 Diseño de las páginas

**Archivos:** `templates/fragments/layout.html` y `static/css/styles.css`.

- **`layout.html`** tiene las partes comunes: el `<head>`, el menú, los mensajes y el pie. Cada página las incluye con `th:replace`, así no se repite el mismo HTML en todas.
- **El menú cambia según quién entró** (`sec:authorize`): un visitante ve "Ingresar" y "Crear cuenta", y cada rol ve el link a su espacio.
- **`styles.css`** cambia los colores de Bootstrap por los de la marca: verde monte, arena y un acento terracota.

## 2.7 Registro de turistas y prestadores (formularios)

**Archivos:** `controllers/RegisterController`, `services/TouristService`, `services/LocationService`, `dto/TouristRegistrationDTO` y `templates/register/*.html`.

- **Patrón de formulario** ⭐, que se repite en todo el proyecto:
  1. El `@GetMapping` pone un DTO vacío en el modelo (`model.addAttribute("form", new ...DTO())`) y muestra la página.
  2. En el HTML, `th:object="${form}"` y `th:field="*{campo}"` conectan cada input con un campo del DTO.
  3. El `@PostMapping` recibe el DTO lleno con `@ModelAttribute` y llama al service.
  4. **Si sale bien:** `redirect:` a otra página, con un mensaje "flash" (`RedirectAttributes`) que se muestra una sola vez. Se redirige para que, si el usuario aprieta F5, no se mande el formulario de nuevo.
  5. **Si sale mal:** se atrapa la excepción, se pone el mensaje en `error` y se vuelve a mostrar el mismo formulario con lo que había escrito.
- **`TouristService.registerTourist`:**
  - valida los datos del Acta 1 (punto 4.1);
  - controla que no se repitan el email ni el documento;
  - cifra la contraseña con BCrypt.

  Es el mismo esquema que `ProviderService.registerProvider` de Lorenzo.
- **El formulario del prestador** usa el mismo `ProviderService.registerProvider` que la API REST. La lógica está una sola vez, aunque haya dos formas de usarla.
- **Desplegable de localidades:** `LocationService.getLocalitiesByDepartment()` arma un `Map` de departamento a localidades. El HTML lo muestra con `<optgroup>`: los departamentos son los títulos y las localidades, las opciones.

## 2.8 Bandeja de solicitudes del administrador

**Archivos:** `services/BusinessService`, `controllers/AdminController` y `templates/admin/*.html`.

- **Sale del Acta 1, punto 4.2** ⭐: el emprendimiento queda pendiente hasta que un administrador lo aprueba. Recién ahí puede publicar actividades y aparecer en el portal.
- **`approveBusiness`:** pasa el estado a `APPROVED` y guarda la fecha y qué administrador lo resolvió.
- **`rejectBusiness`:** lo mismo, pero con `REJECTED`. **Exige un motivo**, que después ve el prestador en su panel.
- **Solo se resuelven solicitudes pendientes.** No se puede aprobar dos veces ni aprobar algo que ya se rechazó.
- **`Principal principal`:** en el controller es "quién está logueado". `principal.getName()` devuelve su email, y con eso el service sabe qué administrador resolvió.
- **El panel del prestador** (`/provider`) muestra cada emprendimiento con su estado y, si fue rechazado, el motivo.

## 2.9 Actividades, fechas, tipos y equipamiento

**Entidades** (todas siguen el MER):

| MER | Clase | Qué es |
|---|---|---|
| ACTIVIDAD | `Activity` | La experiencia que ofrece un emprendimiento: nombre, descripción, lugar, duración, precio y condiciones. |
| INSTANCIA_ACTIVIDAD | `ActivityInstance` | Una fecha y hora concreta de esa actividad. Una actividad tiene muchas. |
| TIPO_ACTIVIDAD | `ActivityType` | Categoría (senderismo, cabalgata…). |
| ACTIVIDAD_TIPO | tabla `activity_activity_types` | Relación muchos a muchos entre actividad y tipo. |
| EQUIPAMIENTO | `Equipment` | Casco, chaleco salvavidas… |
| ACTIVIDAD_EQUIPAMIENTO | `ActivityEquipment` | Relación entre actividad y equipamiento, con el dato `providedByProvider`. |

- **`@ManyToMany` vs. entidad intermedia** ⭐:
  - Actividad–tipo es un `@ManyToMany` común, porque la tabla del medio solo guarda las dos claves.
  - Actividad–equipamiento necesita una **clase propia** (`ActivityEquipment`), porque la relación tiene un dato extra: si el equipamiento lo da el prestador o lo trae el turista. Un `@ManyToMany` no puede guardar datos propios.
- **`BigDecimal` para el precio** ⭐: con `double`, 0.1 + 0.2 da 0.30000000000000004. Para plata siempre se usa `BigDecimal`, que es exacto.
- **Campos de `Activity` que salen de la letra y del acta:**
  - `minimumAge`: requisito de edad (letra). Se controla al reservar, con la fecha de nacimiento del turista.
  - `bookingLimitMinutes`: hasta cuántos minutos antes del inicio se puede reservar (letra). 0 = hasta la hora de inicio.
  - `allowsCancellation` y `cancellationMinHours`: si el turista puede cancelar y con cuánta anticipación (Acta 1, punto 4.5).
  - `published`: el prestador puede ocultar una actividad sin borrarla.
- **`ActivityInstance.maxCapacity` es opcional:** el acta dice que no hay cupos, pero que se va a volver a consultar (punto 4.3). Por eso el campo existe y, si queda vacío, la fecha no tiene cupo.
- **`ActivityInstance.getBookingDeadline()`** calcula la hora límite para reservar: inicio menos los minutos límite.
- **`cascade = CascadeType.ALL`:** al guardar la actividad se guardan también sus fechas y su equipamiento.
- **`orphanRemoval = true`:** si se saca un equipamiento de la lista, se borra de la base.

**`ActivityService`** (lo que hace el prestador):
- `createActivity` y `updateActivity`:
  - validan los datos;
  - **solo dejan publicar si el emprendimiento es del prestador logueado y está APROBADO** ⭐ (Acta 1, punto 4.2).
- `addInstance`: agrega una fecha. Tiene que ser futura, y el cupo, si se pone, tiene que ser de al menos 1.
- `getActivityOfProvider`: si la actividad es de otro prestador, responde "no encontrada". Así nadie puede ver ni editar actividades ajenas cambiando el número en la URL ⭐.

**`ActivityTypeService` y `EquipmentService`** (lo que hace el administrador): alta, modificación y baja.
- No se puede repetir un nombre.
- No se puede borrar algo que ya usa alguna actividad, para no dejar actividades "colgadas".

**Pantallas:**
- `provider/activity-form.html`: el mismo formulario sirve para crear y para editar (`ActivityDTO.from(activity)` lo llena con los datos actuales).
- `provider/activity.html`: detalle de la actividad, con sus fechas y el formulario para agregar más.
- `admin/catalog.html`: una sola plantilla para los dos ABM (tipos y equipamiento). Cambian el título y la URL.

**Datos de ejemplo** (`DataLoader`):
- 9 tipos de actividad y 10 equipamientos;
- 4 emprendimientos aprobados en Rocha, Lavalleja, Colonia y Maldonado, y 1 pendiente;
- 7 actividades con fechas en los próximos días. Las fechas se calculan desde el día en que arranca la app, así la demo siempre tiene fechas futuras.

## 2.10 Portal público y buscador

**Archivos:** `services/PortalService`, `controllers/IndexController`, `dto/ActivitySearchDTO`, `templates/index.html`, `activities.html`, `activity.html` y `fragments/cards.html`.

- **Es todo público** ⭐. La letra dice que el visitante explora "sin necesidad de registrarse". En `SecurityConfig` estas URLs no están protegidas.
- **Qué actividades se ven:** las que cumplen las tres condiciones:
  - están publicadas;
  - su emprendimiento está **aprobado**;
  - tienen al menos una fecha en la que todavía se puede reservar.
- **`Activity.getUpcomingInstances()`:** devuelve las fechas programadas (no canceladas) cuyo cierre de reservas todavía no pasó. Lo usan el portal, las tarjetas y el detalle.
- **Buscador (`searchActivities`)** ⭐:
  - Los filtros de la letra son destino, tipo, fecha, precio y palabra clave.
  - Todos son opcionales: cada `filter(...)` del stream deja pasar todo si ese filtro está vacío.
  - Los filtros viajan en la URL (`/activities?departmentId=14&typeId=3`). Así se puede compartir el link de una búsqueda o volver atrás con el navegador.
  - **Si preguntan por qué se filtra en Java y no en SQL:** con pocos datos es más simple de leer y de probar. Si la plataforma creciera mucho, convendría pasar los filtros a una consulta del repositorio con `@Query`.
- **Portada:** muestra las 6 actividades con la fecha más cercana (`getFeaturedActivities`).
- **Detalle:** si alguien pone en la URL el número de una actividad oculta o de un emprendimiento no aprobado, responde "no encontrada" (404).
- **`@ExceptionHandler`:** en lugar de repetir `try/catch` en cada método, un método del controller atrapa todas las `EntityNotFoundException` y muestra la página de error con código 404.
- **Fragmentos reutilizables (`fragments/cards.html`):**
  - la tarjeta de actividad se escribe una vez y se usa en la portada y en el buscador;
  - la portada de color de cada actividad depende de su tipo, a través de un atributo `data-type` que lee el CSS.

## 2.11 Reservas ⭐

**Archivos:** `entities/Booking`, `BookingStatus`, `CancelledBy`, `services/BookingService` y `controllers/TouristController`.

**Recorrido del turista:**

```
Detalle de la actividad → elige fecha → /tourist/book/{fecha}
  → si no inició sesión, Spring Security lo manda al login y después lo trae de vuelta
  → elige cantidad de personas y observaciones → se crea la reserva PENDIENTE
  → /tourist/bookings/{id}/pay → paga → CONFIRMADA → ve su QR
```

**Reglas que controla `createBooking`** (cada una sale de la letra o del acta):

| Regla | De dónde sale |
|---|---|
| La fecha está programada (no cancelada) y la actividad es visible | Letra |
| Todavía no pasó el cierre de reservas (`bookingLimitMinutes`) | Letra: "hasta cuánto tiempo está permitido realizar la reserva" |
| Al menos 1 persona | Lógica |
| El turista tiene la edad mínima de la actividad (se calcula con su fecha de nacimiento) | Acta 1, punto 4.1 |
| Si la fecha tiene cupo, alcanzan los lugares | Acta 1, punto 4.3 (cupo opcional) |
| Total = precio × personas, calculado en el servidor (no se confía en lo que manda el navegador) | Seguridad |
| La reserva queda PENDIENTE y vence en 2 horas | Acta 1, punto 4.8 |

- **Lugares ocupados (`getTakenPlaces`):** cuentan las reservas confirmadas, las realizadas y las pendientes que todavía no vencieron. Por eso una reserva pendiente "retiene el lugar", como dice el acta.
- **Número de reserva:** `UM-` seguido de 6 caracteres al azar (`SecureRandom`). No usa letras que se confunden (O/0, I/1) y se verifica que no esté repetido.
- **Estados** (Acta 1, punto 4.8): `PENDING` → `CONFIRMED` → `COMPLETED`, o `CANCELLED` en cualquier momento antes. `cancelledBy` guarda quién canceló: el turista, el prestador o el sistema.

**Vencimiento automático** ⭐:
- `cancelExpiredBookings()` tiene `@Scheduled(fixedRate = 60000)`, así que Spring la ejecuta sola cada minuto.
- Cancela las reservas pendientes que pasaron las 2 horas sin pagar y así libera los lugares.
- Para que funcione, la clase principal tiene `@EnableScheduling`.

**Cancelaciones:**
- **El turista** (`cancelByTourist`) puede cancelar si la actividad lo permite y si falta más tiempo que la anticipación mínima (Acta 1, punto 4.5). Una pendiente siempre la puede cancelar.
- **El prestador** (`cancelInstance`) cancela una fecha entera, y se cancelan todas sus reservas activas (Acta 1, punto 4.6).
- **Pendiente con el cliente:** el acta deja sin definir los motivos de cancelación y el porcentaje de devolución de cada uno. Por eso no se implementó el reembolso.

## 2.12 Pagos: integración con un servicio externo ⭐⭐

**Archivos:** `services/PaymentGatewayClient`, `simulator/FakePaymentGatewayController`, `entities/Payment` y `services/BookingService.payBooking`.

Es el punto de la letra sobre integración con un sistema externo, y uno de los más probables de que pregunten.

**Cómo está armado:**

```
BookingService.payBooking
   → PaymentGatewayClient.charge(...)        (nuestro código)
      → POST http://.../api/v1/charges       (HTTP de verdad, con RestClient)
         → servicio de pagos externo         (para la demo, el simulador)
```

- **`PaymentGatewayClient`** es lo único que sabe hablar con el servicio de pagos. Si mañana se usa Mercado Pago, se cambia esta clase y el resto queda igual.
- **La dirección del servicio** se configura en `application.properties` (`payment.gateway.url`), no está escrita en el código.
- **El simulador** (`FakePaymentGatewayController`) hace de "otra empresa". Responde según la tarjeta: aprobado (200), rechazado (402) o caído (503). Ver las tarjetas de prueba en el README.

**Los tres resultados posibles** (la letra pide manejarlos "de manera coherente"):

| Qué pasa | Qué hace el sistema |
|---|---|
| **Aprobado** | El pago queda `APPROVED` y la reserva pasa a `CONFIRMED`. |
| **Rechazado** (por ejemplo, sin fondos) | El pago queda `REJECTED`. La reserva **sigue pendiente**, y el turista puede probar otra tarjeta dentro de las 2 horas. |
| **El servicio no responde** (error 5xx, se cae la conexión o pasan más de 5 segundos) | El pago queda `ERROR`. La reserva **sigue pendiente** y se le pide al turista que reintente. **Nunca se confirma una reserva sin pago aprobado** (Acta 1, punto 4.7). |

**Detalles que conviene saber explicar:**
- **Timeout de 5 segundos:** si el servicio externo se cuelga, el turista no se queda esperando para siempre.
- **Cada intento de pago queda guardado**, salga bien o mal. Sirve para auditoría y para mostrarle al turista qué pasó.
- **De la tarjeta solo se guardan la marca y los últimos 4 números**, nunca el número completo ni el CVV.
- **`@Transactional(noRollbackFor = ...)`:** si el pago falla, se lanza una excepción, pero el registro del intento fallido se guarda igual.

## 2.13 Código QR y asistencia

**Archivos:** `services/QrCodeService`, `TouristController.qrCode` y `ProviderWebController.checkIn`.

- **Sale del Acta 1, punto 4.10:** confirmada la reserva, se genera un QR con el número de reserva. El día de la actividad, el prestador lo escanea y la reserva pasa a REALIZADA.
- **`QrCodeService`** arma la imagen PNG con la librería **ZXing**.
- **`/tourist/bookings/{id}/qr.png`** devuelve esa imagen. `@ResponseBody` hace que el controller devuelva los bytes de la imagen en lugar del nombre de una página.
- **El prestador** escribe (o escanea) el número en "Reservas → Registrar asistencia". `checkIn` controla cuatro cosas:
  - que la reserva sea de una actividad suya;
  - que esté confirmada (pagada);
  - que sea el día de la actividad;
  - que no se haya registrado antes.
- **El mail de confirmación** se muestra como mensaje en pantalla. Mandar mails de verdad necesita un servidor de correo y queda como mejora.

## 2.14 Valoraciones

**Archivos:** `entities/Rating` y `services/RatingService`.

- **Se puede valorar si se cumplen tres cosas:**
  - la reserva se pagó;
  - la fecha de la actividad ya pasó;
  - todavía no la valoró (Acta 1, punto 4.9).
- **Una valoración por reserva:** `@OneToOne` con `unique = true`.
- **Son públicas:** el detalle de la actividad muestra el promedio con estrellas y los comentarios, como pide la letra.

## 2.15 API pública para otras plataformas ⭐

**Archivos:** `controllers/PublicApiRestController`, `dto/ActivitySummaryDTO` y `dto/BusinessVerificationDTO`.

La letra pide que el sistema exponga una API con 5 operaciones:

| Operación (letra) | Endpoint |
|---|---|
| Listado de todos los emprendimientos | `GET /api/v1/businesses` (solo los aprobados) |
| Actividades de un día por departamento o localidad | `GET /api/v1/activities?date=2026-10-20&departmentId=14` (o `&localityId=22`) |
| Top 3 mejor puntuadas del mes por departamento | `GET /api/v1/activities/top?departmentId=14&month=2026-10` |
| Verificar emprendimiento | `GET /api/v1/businesses/{id}/verify`: devuelve `verified: true` si está aprobado |
| Buscar actividad por palabra clave | `GET /api/v1/activities/search?keyword=kayak` |

- **Documentación:** con la app corriendo, en **http://localhost:8080/swagger-ui.html**. Swagger la genera sola a partir del código (librería `springdoc-openapi`). Las anotaciones `@Tag`, `@Operation` y `@Parameter` agregan las descripciones. Desde esa página se puede probar cada endpoint, y cumple el punto de la letra "API REST con detalle de endpoints y su documentación".
- **Top 3 del mes:** se promedian solo las valoraciones hechas en ese mes. Si no se indica el mes, se usa el actual.
- **Devuelve DTOs, como el resto de la API:** nunca se exponen las entidades.

## 2.16 Clima

**Archivos:** `services/WeatherService`, `dto/OpenMeteoResponseDTO` y `dto/WeatherDTO`.

- **La letra dice que "sería conveniente"** mostrar el pronóstico para la fecha y el lugar de la actividad.
- **Usa Open-Meteo**, una API pública, gratis y sin clave. Para eso, cada localidad tiene latitud y longitud (están en `data.sql`).
- **Hace una sola consulta por actividad**, con el rango de fechas, y reparte el resultado en cada fecha. Open-Meteo da pronóstico para unos 15 días; las fechas más lejanas se muestran sin clima.
- **Si no hay internet o la API falla,** la página se muestra igual, sin el clima, y queda un aviso en el log. Es el mismo criterio que pide la letra para los servicios externos: que una falla de afuera no rompa el sistema.
- **`OpenMeteoResponseDTO`:**
  - `@JsonProperty("weather_code")` conecta el nombre del JSON con el del campo Java;
  - `@JsonIgnoreProperties(ignoreUnknown = true)` ignora los datos que no se usan.
- **El código de clima** viene en el estándar WMO: 0 = despejado, 61 = lluvia, 95 = tormenta, etc. `toWeather` lo traduce a texto y a un ícono.

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

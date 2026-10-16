# Uruguay Mnatural (Proyecto TIC 2026, equipo TeroBytes)

Plataforma que conecta turistas con prestadores de actividades turísticas en Uruguay. Es un proyecto universitario (Universidad de Montevideo).

## Contexto obligatorio

Antes de proponer cambios, leé:
- `docs/HISTORIAL.md`: qué se hizo, decisiones tomadas, equivalencia MER → código y estado actual.
- `docs/GUIA_DEL_CODIGO.md`: cómo funciona cada parte del código.
- `docs/PLAN.md`: qué falta y cómo se pensaba seguir.
- `docs/requerimientos/*.txt`: letra del proyecto y actas con el cliente. Los PDF originales están al lado.
- `docs/diagramas/`: MER y modelo relacional.

## Cómo trabajar en este proyecto

- **No inventes requisitos.** Si algo no está en la letra, las actas o el MER, preguntá.
- **No avances solo.** El desarrollador decide el ritmo. Preguntá si quiere ir paso a paso escribiendo él el código (con vos explicando y revisando) o si prefiere que vos escribas.
- **Código en inglés, conversación en español.** La letra exige nombres en inglés.
- **Seguí el estilo del curso.** La referencia es el ejemplo de Moodle `demospring_thymeleaf`:
  - paquetes `entities`, `repository`, `services`, `controllers`, `dto`, `exceptions`;
  - Lombok;
  - `@Autowired` en campos;
  - excepciones checked propias;
  - REST bajo `/api/v1/...` con `ResponseEntity`.
- **Respetá MVC en capas.** Controller → Service → Repository. Las entidades no se devuelven directo por la API: se usan DTOs.

## Stack

- Spring Boot 4.1.1, Java 25 y Maven Wrapper.
- PostgreSQL (Supabase o local). La conexión va en `.env` (copiar de `.env.example`). Los tests usan H2 en memoria.
- Thymeleaf + Bootstrap (webjars), Spring Security (login por roles, BCrypt), Lombok, springdoc (Swagger) y ZXing (QR).
- Servicios externos: pagos (simulador en `simulator/`, llamado por HTTP) y clima (Open-Meteo).

## Comandos

- Tests: `./mvnw test`. Requiere `JAVA_HOME` apuntando a un JDK 25.
- Correr la app: `./mvnw spring-boot:run`, o desde IntelliJ con `Proyectotic2026Application`. Se abre en http://localhost:8080.
- Documentación de la API: http://localhost:8080/swagger-ui.html

## Documentos

- `docs/GUIA_DEL_CODIGO.md`: explicación de todo el código, sección por sección. Actualizarla con cada cambio.
- `docs/GUION_DEMO.md`: recorrido para mostrar el sistema.

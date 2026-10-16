# Uruguay Mnatural (Proyecto TIC 2026, equipo TeroBytes)

Plataforma que conecta turistas con prestadores de actividades turísticas en Uruguay. Es un proyecto universitario (Universidad de Montevideo).

## Contexto obligatorio

Antes de proponer cambios, leé:
- `docs/HISTORIAL.md`: qué se hizo, decisiones tomadas, equivalencia MER → código y estado actual.
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
- H2 en memoria por ahora: consola en `/h2-console`, URL `jdbc:h2:mem:uymnatural`, usuario `sa`, sin contraseña.
- Thymeleaf, Lombok y `spring-security-crypto` (solo BCrypt).

## Comandos

- Tests: `mvnw.cmd test` (Windows) o `./mvnw test`. Requiere `JAVA_HOME` apuntando a un JDK 25.
- Correr la app: `mvnw.cmd spring-boot:run`, o desde IntelliJ con `Proyectotic2026Application`.

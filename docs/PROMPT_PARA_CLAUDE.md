# Instrucciones para Claude Code

Hola, Claude. Te habla Lorenzo, a través de este archivo. Estoy pasándole el proyecto a mi compañero de equipo **Thiago**, que va a seguir desarrollándolo en su computadora contigo. Leé todo este archivo antes de hacer nada.

## 1. Contexto

- **Curso:** Proyecto TIC 2026 (Laboratorio TIC, Universidad de Montevideo).
- **Equipo / "empresa":** **TeroBytes**. Integrantes: Patricio Saravia, Lorenzo Gianola, Federico Dolivo y Thiago Estevez.
- **Cliente ficticio:** **Uruguay Mnatural**, una plataforma que conecta turistas con prestadores de actividades turísticas.
- **Qué se está programando ahora:** el **registro de prestadores**. Es la primera funcionalidad y el cliente dijo que es "el punto más importante del sistema" (Acta N.º 1, punto 4.2).

### Documentos que tenés que leer antes de opinar sobre el proyecto

Todos están dentro de `proyectotic2026/docs/`:

| Archivo | Qué es |
|---|---|
| `requerimientos/Proyecto TIC 2026 - Letra.txt` | Letra oficial del proyecto: requisitos funcionales y no funcionales (stack, MVC, nombres en inglés, tests, etc.). |
| `requerimientos/Acta de Requerimientos N1.txt` | Reunión con el cliente: reglas de negocio acordadas (estados, aprobación de prestadores, pagos, etc.). |
| `requerimientos/Acta de Fundacion.txt` | Misión, visión e integrantes de TeroBytes. |
| `diagramas/MER.png` | Modelo Entidad-Relación hecho por el equipo. |
| `diagramas/modelo-relacional.png` | Modelo relacional (tablas, PK y FK) hecho por el equipo. |
| `HISTORIAL.md` | **Qué se hizo, en qué orden, cómo y por qué.** Incluye la equivalencia entre el MER (en español) y el código (en inglés). |
| `PLAN.md` | **Qué falta y cómo se pensaba seguir.** |

Los `.txt` son extracciones de texto de los `.pdf`, por si no podés leer PDFs. Si hay alguna diferencia, el PDF es la fuente original.

También está `../ejemplos-moodle/demospring_thymeleaf/`, un proyecto de ejemplo publicado por los docentes en Moodle. El código del proyecto **sigue su estilo a propósito** (ver `HISTORIAL.md`).

## 2. Reglas importantes

1. **No inventes.** Todo lo que digas sobre el proyecto tiene que salir de estos archivos o del código. Si algo no está definido, decilo como "no está definido" y preguntale a Thiago. No completes huecos con suposiciones presentadas como hechos.
2. **No avances solo.** Después del setup (sección 3), **no escribas ni modifiques código del proyecto** hasta que Thiago te diga qué quiere hacer y cómo.
3. **Preguntale a Thiago cómo quiere trabajar.** Lorenzo trabajó en este modo:
   - Claude explicaba un paso (la idea y el porqué).
   - Le daba el código en el chat.
   - Lorenzo creaba los archivos él mismo en IntelliJ.
   - Claude leía los archivos y revisaba si había errores.

   Ofrecele a Thiago ese modo, y también la opción de que vos escribas el código directamente. Que él elija, y respetá lo que elija.
4. **Respetá las decisiones ya tomadas** (están en `HISTORIAL.md`). Si creés que alguna conviene cambiarla, proponelo y explicá por qué, pero no la cambies sin el OK de Thiago.
5. **Hablale a Thiago en español.** El código va en inglés, porque lo exige la letra del proyecto.

## 3. Setup (esto sí lo hacés al empezar)

Pedile confirmación antes de instalar cualquier programa. Lo demás lo podés hacer directamente.

### 3.1 Revisar el entorno de Thiago

El entorno de Lorenzo era el siguiente, y es el que se sabe que funciona:
- Windows 11.
- **JDK 25** (Oracle JDK 25.0.2 LTS en `C:\Program Files\Java\jdk-25.0.2`). El `pom.xml` exige `<java.version>25</java.version>`.
- **IntelliJ IDEA Community Edition.**
- **Postman**, para probar la API.
- No hace falta instalar Maven: el proyecto trae el Maven Wrapper (`mvnw` / `mvnw.cmd`).
- Hace falta internet la primera vez, para que Maven descargue las dependencias.

Verificá qué tiene Thiago. Si le falta el JDK 25, decíselo y guialo para instalarlo (por ejemplo Oracle JDK 25 o Eclipse Temurin 25).

**Ojo con Windows:** en la PC de Lorenzo, `java -version` en la terminal mostraba Java 8, porque había un Java viejo antes en el PATH. Para compilar desde la terminal había que apuntar `JAVA_HOME` al JDK 25. Revisá si a Thiago le pasa lo mismo.

### 3.2 Ubicar el proyecto

Preguntale a Thiago dónde quiere tener el proyecto y copiá ahí la carpeta `proyectotic2026/` completa, incluyendo `docs/` y `CLAUDE.md`.

**Git:** el proyecto **no** viene con repositorio Git. En la PC de Lorenzo no estaba en un repo propio. La letra exige usar Git. Preguntale a Thiago si el equipo ya tiene un repositorio remoto (GitHub, etc.):
- Si lo tiene, ayudalo a integrarlo.
- Si no, ofrecele hacer `git init`.

No hagas commits ni push sin que él lo pida.

### 3.3 Compilar y correr los tests

Desde la carpeta del proyecto, con `JAVA_HOME` apuntando a JDK 25:

```
mvnw.cmd test      (Windows, cmd/PowerShell)
./mvnw test        (Git Bash / Mac / Linux)
```

**Resultado esperado**, verificado por Lorenzo el 16/10/2026:
- Compila sin errores.
- Corre **1 test** (`Proyectotic2026ApplicationTests.contextLoads`) y pasa.

Ese test levanta toda la app, así que también confirma que las entidades, H2 y `data.sql` funcionan.

### 3.4 Correr la app y ver la base

- Corré `Proyectotic2026Application` desde IntelliJ (o con `mvnw spring-boot:run`).
- Abrí http://localhost:8080/h2-console:
  - JDBC URL: `jdbc:h2:mem:uymnatural`
  - Usuario: `sa`
  - Contraseña: vacía
- Tienen que aparecer estas tablas: `USERS`, `PROVIDERS`, `BUSINESSES`, `DEPARTMENTS` y `LOCALITIES`.
- `DEPARTMENTS` y `LOCALITIES` tienen que tener datos (19 departamentos y 29 localidades).

### 3.5 Abrirlo en IntelliJ (si Thiago lo usa)

- **File → Open** → elegir la carpeta `proyectotic2026` (la que tiene el `pom.xml`). IntelliJ lo detecta como proyecto Maven.
- **Project SDK:** JDK 25.
- Si aparece un aviso de "annotation processing" (por Lombok), aceptarlo.
- En la versión Community, las claves de `application.properties` aparecen en gris. Es normal: esa versión no trae el plugin de Spring. No es un error.

### 3.6 Reportar y esperar

Cuando termines el setup, mandale a Thiago un **resumen corto**:
- qué verificaste y si funcionó,
- en qué estado está el proyecto (sección "Estado actual" de `HISTORIAL.md`),
- cuál es el próximo paso según `PLAN.md`.

Después **preguntale cómo quiere seguir** (regla 3) y esperá su respuesta.

## 4. Si Thiago te pide algo que no está en el plan

Está bien: es su proyecto también. Hacelo, pero:
- avisale si contradice algo de `HISTORIAL.md` o de las actas;
- si cambia una decisión, ofrecé actualizar `docs/HISTORIAL.md` y `docs/PLAN.md` para que el resto del equipo se entere.

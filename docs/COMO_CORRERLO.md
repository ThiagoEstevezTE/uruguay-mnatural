# Cómo hacer andar el proyecto en tu compu

Esta guía explica, paso a paso, cómo bajar Uruguay Mnatural de GitHub y dejarlo funcionando en http://localhost:8080, con la base de datos y los datos de prueba ya cargados. Sirve para Windows y para Mac.

**Qué vas a instalar:** solo dos programas, Docker Desktop e IntelliJ IDEA.

**Qué NO hace falta:**
- instalar PostgreSQL;
- crear la base de datos.

Para pagar con Mercado Pago hace falta un archivo `.env` con sus claves (se copia de `.env.example` y las claves las pasa Thiago). Sin ese archivo, la app arranca igual con `PAYMENT_PROVIDER=simulator` en el `.env` y las tarjetas de prueba del simulador.

La base la levanta Docker sola.

**Cuánto tarda:** la primera vez, entre 30 y 45 minutos, casi todo esperando descargas. Después, cada vez que quieras usar la app, menos de un minuto.

Cuando ya la tengas andando, la guía de uso explica qué hace cada pantalla: [GUIA_DE_USO.md](GUIA_DE_USO.md).

---

## Parte 1: lo que se hace una sola vez

### Paso 1. Instalar Docker Desktop

Docker es un programa que corre la base de datos PostgreSQL adentro de una "cajita" en tu compu, ya configurada. Así no tenés que instalarla ni configurarla a mano.

1. Entrá a https://www.docker.com/products/docker-desktop/ y tocá el botón de descarga:
   - **Windows:** *Download for Windows – AMD64*. Es la opción que va en casi todas las compus.
   - **Mac:** primero fijate qué chip tiene tu Mac. Tocá la manzana  de arriba a la izquierda → *Acerca de esta Mac*:
     - si dice **Apple M1, M2, M3 o M4**, bajá *Download for Mac – Apple Silicon*;
     - si dice **Intel**, bajá *Download for Mac – Intel Chip*.
2. Instalalo.
   - **Windows:**
     1. Abrí el archivo que bajaste, `Docker Desktop Installer.exe`.
     2. Dejá marcada la opción **Use WSL 2** y tocá **OK**.
     3. Al final tocá **Close and restart**: la compu se reinicia.
   - **Mac:**
     1. Abrí el archivo `Docker.dmg`.
     2. En la ventana que aparece, arrastrá la ballena de **Docker** a la carpeta **Aplicaciones**.
     3. Abrí **Docker** desde Aplicaciones. Si pregunta si querés abrirlo, decí que sí.
     4. Si te pide la contraseña de la Mac, escribila.
3. La primera vez que se abre:
   1. Aceptá los términos (**Accept**).
   2. Si ofrece la configuración recomendada, elegí **Use recommended settings**.
   3. Cuando pida iniciar sesión, podés tocar **Skip**, o **Continue without signing in**: no hace falta tener cuenta.
   4. Si aparece una encuesta, también podés saltearla.
4. Esperá a que, abajo a la izquierda de la ventana de Docker, diga **Engine running** con un punto verde. Ahí ya está listo.

> **Windows:** si al abrir Docker aparece un aviso de que hay que actualizar WSL, abrí el menú Inicio y buscá **PowerShell**. Hacé clic derecho → **Ejecutar como administrador**, escribí `wsl --update`, apretá Enter, esperá a que termine y volvé a abrir Docker.

### Paso 2. Instalar IntelliJ IDEA

IntelliJ es el programa donde se abre el código y desde donde se prende la app.

1. Bajalo de https://www.jetbrains.com/idea/download/. La versión gratis alcanza. Con el mail de la facultad también se puede pedir la licencia gratis de estudiante en https://www.jetbrains.com/student/.
2. Instalalo con las opciones que vienen marcadas.
3. Si ya lo tenías instalado, actualizalo: el proyecto usa Java 25 y las versiones viejas de IntelliJ no lo reconocen.

### Paso 3. Aceptar la invitación de GitHub

El repositorio es privado, así que solo pueden entrar los que están invitados.

1. Buscá en tu mail uno de GitHub que dice que te invitaron a `ThiagoEstevezTE/uruguay-mnatural`. Fijate también en Spam.
2. Tocá **View invitation** y después **Accept invitation**.
3. Si no tenés cuenta de GitHub, te va a pedir crear una. Hacelo con el mismo mail al que te llegó la invitación.

### Paso 4. Bajar el proyecto a tu compu

1. Abrí IntelliJ.
   - En la pantalla de bienvenida tocá **Clone Repository**. En algunas versiones el botón se llama **Get from VCS**.
   - Si ya tenés otro proyecto abierto, andá a **File → New → Project from Version Control**.
2. En **URL** pegá esto:

   ```
   https://github.com/ThiagoEstevezTE/uruguay-mnatural.git
   ```

   En **Directory** queda la carpeta donde se va a guardar. Podés dejar la que viene.
3. Tocá **Clone**.
   - Si dice que no encuentra Git, tocá **Download and Install**, esperá y volvé a tocar **Clone**.
   - Si pide usuario y contraseña, o iniciar sesión en GitHub, tocá **Log in via GitHub**. Se abre el navegador: tocá **Authorize** y volvé a IntelliJ.
4. Cuando pregunte si confiás en el proyecto, tocá **Trust Project**.
5. Esperá a que IntelliJ termine de cargar. La primera vez baja todo lo que usa el proyecto y puede tardar varios minutos. Se ve en una barrita abajo a la derecha: cuando desaparece, terminó.

### Paso 5. Elegir Java 25

1. Andá a **File → Project Structure** y, a la izquierda, a **Project**.
2. Mirá el campo **SDK**:
   - si ya dice una versión **25**, no hagas nada;
   - si no, abrí la lista, elegí **Download JDK…**, poné versión **25** y cualquier proveedor (*Vendor*), y tocá **Download**.
3. Tocá **OK**.

---

## Parte 2: lo que se hace cada vez que querés usar la app

### Paso 1. Abrir Docker Desktop

Abrí Docker y esperá a que diga **Engine running**.

### Paso 2. Prender la base de datos

1. En IntelliJ abrí la pestaña **Terminal**. Está abajo a la izquierda y el ícono es un cuadradito con `>_`. También se abre con **View → Tool Windows → Terminal**.
2. Escribí esto y apretá Enter:

   ```
   docker compose up -d
   ```

3. La primera vez tarda uno o dos minutos, porque baja PostgreSQL. Termina cuando dice `Started` o `Running`. Las veces siguientes es casi inmediato.

### Paso 3. Prender la app

1. En el panel de la izquierda abrí estas carpetas: `src` → `main` → `java` → `uy.edu.um.proyectotic2026`.
2. Hacé doble clic en `Proyectotic2026Application`.
3. Tocá el **▶ verde** que aparece a la izquierda de `public class Proyectotic2026Application` y elegí **Run**.
4. Abajo se abre la consola, llena de texto. La app está lista cuando aparece `Started Proyectotic2026Application`.

La primera vez la app crea sola las tablas y carga los datos de prueba.

### Paso 4. Abrir la página

1. En el navegador entrá a http://localhost:8080.
2. Tocá **Ingresar** y entrá, por ejemplo, con `turista@demo.uy`. Todas las cuentas de prueba tienen la contraseña `demo1234`; están en la [guía de uso](GUIA_DE_USO.md#cuentas-y-contraseñas).

Si entraste y ves "Mis reservas" arriba, ya está todo funcionando.

### Al terminar

1. Pará la app con el **■ rojo** de la consola de IntelliJ.
2. Apagá la base escribiendo esto en la Terminal:

   ```
   docker compose down
   ```

   Los datos quedan guardados para la próxima vez.
3. Cerrá Docker Desktop:
   - **Mac:** clic en la ballena de la barra de arriba → **Quit Docker Desktop**;
   - **Windows:** clic derecho en la ballena de la barra de abajo → **Quit Docker Desktop**.

   Conviene cerrarlo porque usa bastante memoria.

---

## Cosas que conviene saber

- **Traer lo último que subieron los demás:** en IntelliJ, **Git → Pull** y después **OK**. Si la app estaba prendida, pará y volvé a darle ▶.
- **Volver a los datos de prueba del principio.** Las fechas de ejemplo se calculan desde el día en que se cargaron; por ejemplo, la reserva "de hoy". Para cargarlas de nuevo:
  1. Pará la app.
  2. En la Terminal escribí:

     ```
     docker compose down -v
     docker compose up -d
     ```

  3. Volvé a darle ▶.

  Ojo: esto **borra todo lo que hiciste** en tu base y la deja como nueva.
- **Cada uno tiene su propia base.** Lo que reserves o cargues en tu compu no lo ven los demás, y lo que hagan ellos tampoco te cambia nada a vos.
- **El día de la presentación** se usa la compu de Thiago, conectada a la base del equipo en Supabase. Esta guía es para que cada uno pueda practicar y revisar el código en su compu.

## Si algo no anda

| Qué pasa | Qué hacer |
|---|---|
| `docker: command not found` o `"docker" no se reconoce como un comando` | Docker Desktop no está abierto o no terminó de instalarse. Abrilo, esperá a que diga **Engine running**, cerrá la pestaña Terminal de IntelliJ y abrí una nueva. |
| `Cannot connect to the Docker daemon` o `error during connect` | Docker está instalado pero no está prendido. Abrilo y esperá a **Engine running**. |
| `port is already allocated` o `5432 ... already in use` al hacer `docker compose up -d` | Ya tenés un PostgreSQL instalado y prendido en tu compu. Apagalo: en Windows, buscá **Servicios** en el menú Inicio → **postgresql** → **Detener**. Después probá de nuevo. |
| La app no arranca y dice `Connection refused` o `Connection to localhost:5432 refused` | La base no está prendida. Hacé el paso 2 de la Parte 2 (`docker compose up -d`) y volvé a darle ▶. |
| La app no arranca y dice `password authentication failed` | Casi siempre es un PostgreSQL instalado en Windows que contesta en el mismo puerto que el de Docker. Buscá **Servicios** en el menú Inicio → `postgresql-x64-…` → clic derecho → **Detener**, y en sus propiedades poné **Tipo de inicio: Manual**. Si sigue, fijate que el `.env` no tenga líneas `DB_…`. |
| `Port 8080 was already in use` | Ya hay otra app usando ese puerto, seguramente otra copia de esta prendida. Parala con el ■ rojo. |
| Error de Java: `invalid source release: 25` o parecido | Falta elegir Java 25 (Parte 1, paso 5). |
| No aparece el ▶ verde | IntelliJ todavía está cargando: esperá a que se vaya la barrita de abajo a la derecha. Si sigue sin aparecer, hacé clic derecho en `pom.xml` → **Add as Maven Project** (o **Maven → Reload project**). |
| Registrar asistencia dice "La asistencia se registra el día de la actividad" | Los datos de prueba se cargaron otro día. Volvé a los datos del principio (en "Cosas que conviene saber"). |
| No aparece el clima en las fechas | El pronóstico necesita internet. Todo lo demás anda igual. |

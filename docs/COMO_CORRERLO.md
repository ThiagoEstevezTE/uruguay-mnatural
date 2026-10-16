# Cómo hacer andar el proyecto en tu compu

Paso a paso para bajar Uruguay Mnatural de GitHub y verlo funcionando en http://localhost:8080, con la base de datos y todos los datos de prueba cargados. Sirve para Windows y para Mac.

No hace falta instalar PostgreSQL, ni crear la base, ni ningún archivo `.env`: la base la levanta Docker.

---

## Parte 1: lo que se hace una sola vez

### 1. Instalar Docker Desktop

1. Entrá a https://www.docker.com/products/docker-desktop/ y bajá el instalador:
   - **Windows:** *Download for Windows*.
   - **Mac:** *Download for Mac*. Fijate si tu Mac tiene chip de Apple (M1, M2, M3…) o Intel: en  → *Acerca de esta Mac*.
2. Instalalo con las opciones que vienen marcadas. En Windows puede pedir reiniciar la compu.
3. Abrí Docker Desktop. La primera vez pide aceptar los términos; podés saltear el inicio de sesión (*Skip*).
4. Esperá a que abajo a la izquierda diga **Engine running** (en verde).

### 2. Instalar IntelliJ IDEA

1. Bajá **IntelliJ IDEA** desde https://www.jetbrains.com/idea/download/. Con el mail de la facultad se puede pedir la licencia gratis de estudiante; si no, alcanza con la versión *Community*.
2. Si ya lo tenías, actualizalo: el proyecto usa Java 25 y las versiones viejas no lo reconocen.

### 3. Aceptar la invitación de GitHub

Te llega un mail de GitHub con la invitación al repositorio `uruguay-mnatural`. Tocá **View invitation** y después **Accept invitation**. Si no tenés cuenta de GitHub, creala antes.

### 4. Bajar el proyecto

1. Abrí IntelliJ. En la pantalla de inicio tocá **Clone Repository** (o, con un proyecto abierto, **File → New → Project from Version Control**).
2. En **URL** pegá:

   ```
   https://github.com/ThiagoEstevezTE/uruguay-mnatural.git
   ```

3. Tocá **Clone**.
   - Si dice que falta Git, tocá **Download and Install** y volvé a probar.
   - Si pide iniciar sesión en GitHub, tocá **Log in via GitHub** y aceptá en el navegador.
4. Cuando pregunte si confiás en el proyecto, tocá **Trust Project**. Esperá a que termine de cargar (la barra de abajo a la derecha).

### 5. Elegir Java 25

1. Andá a **File → Project Structure → Project**.
2. En **SDK**, si no aparece una versión 25, elegí **Download JDK…**, versión **25**, cualquier proveedor, y tocá **Download**.
3. Tocá **OK**.

---

## Parte 2: lo que se hace cada vez que querés usar la app

### 1. Prender Docker Desktop

Abrilo y esperá a que diga **Engine running**.

### 2. Prender la base de datos

En IntelliJ abrí la pestaña **Terminal** (abajo a la izquierda) y escribí:

```
docker compose up -d
```

La primera vez tarda un poco porque baja PostgreSQL. Termina cuando dice `Started` o `Running`.

### 3. Levantar la app

1. En el panel de la izquierda abrí `src/main/java/uy/edu/um/proyectotic2026/Proyectotic2026Application`.
2. Tocá el **▶ verde** al lado de `public class Proyectotic2026Application` y elegí **Run**.
3. Abajo aparece la consola. La app está lista cuando dice `Started Proyectotic2026Application`.

La primera vez crea las tablas y carga los datos de prueba sola.

### 4. Abrir la página

Entrá a http://localhost:8080. Para ingresar, usá cualquiera de las cuentas de la [guía de uso](GUIA_DE_USO.md#cuentas-y-contraseñas): la contraseña de todas es `demo1234`.

### Al terminar

- Pará la app con el **■ rojo** de la consola.
- Apagá la base con `docker compose down` (los datos quedan guardados para la próxima).
- Cerrá Docker Desktop, que consume bastante memoria.

---

## Cosas que conviene saber

- **Traer lo último que subieron los demás:** en IntelliJ, **Git → Pull**.
- **Volver a los datos de prueba del principio:** las fechas de ejemplo se calculan desde el día en que se cargaron (la reserva "de hoy", por ejemplo). Para cargarlas de nuevo, pará la app y escribí:

  ```
  docker compose down -v
  docker compose up -d
  ```

  Eso **borra todo lo que hiciste** en tu base y la deja como nueva. Después levantá la app otra vez.
- **Cada uno tiene su propia base.** Lo que reserves o cargues en tu compu no lo ven los demás.

## Si algo no anda

| Qué pasa | Qué hacer |
|---|---|
| `docker: command not found` o `docker no se reconoce` | Docker Desktop no está abierto o no se instaló. Abrilo, esperá a **Engine running** y cerrá y volvé a abrir la terminal de IntelliJ. |
| `port is already allocated` o `5432 ... already in use` al hacer `docker compose up -d` | Ya tenés un PostgreSQL instalado y prendido en tu compu. Apagalo (en Windows: *Servicios → postgresql → Detener*) y probá de nuevo. |
| La app no arranca y dice `Connection refused` o `Connection to localhost:5432 refused` | La base no está prendida. Hacé el paso 2 (`docker compose up -d`) y volvé a darle ▶. |
| La app no arranca y dice `password authentication failed` | Hay un archivo `.env` en la carpeta del proyecto con otros datos. Borralo o renombralo. |
| `Port 8080 was already in use` | Ya hay otra app usando ese puerto, seguramente otra copia de esta. Parala con el ■ rojo. |
| Error de Java: `invalid source release: 25` o parecido | Falta elegir Java 25 (Parte 1, paso 5). |
| Registrar asistencia dice "La asistencia se registra el día de la actividad" | Los datos de prueba se cargaron otro día. Volvé a los datos del principio (ver arriba). |

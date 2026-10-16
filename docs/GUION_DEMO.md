# Guion de la demo (unos 10 minutos)

Un recorrido que muestra todas las funciones en orden lógico. Todas las cuentas usan la contraseña `demo1234`.

## Cuentas para entrar

La contraseña de **todas** es `demo1234`. Se entra desde "Ingresar", arriba a la derecha.

| Rol | Usuario | Qué tiene |
|---|---|---|
| Administrador | `admin@uymnatural.uy` | Aprueba o rechaza emprendimientos y maneja los tipos de actividad y el equipamiento. |
| Turista (la principal) | `turista@demo.uy` | Sofía Martínez. Tiene reservas de todo tipo: **UM-H0Y2Q8** (la cabalgata de hoy), **UM-PROX7A** (cabalgata en unos días, se puede cancelar), **UM-KAY4K2** (kayak que ya pasó, para valorar) y **UM-SOF1A9** (Barrio Histórico, realizada y valorada). |
| Turistas extra | `lucia@demo.uy`, `diego@demo.uy`, `ana@demo.uy` | Dejaron las valoraciones que se ven en el detalle de las actividades. |
| Prestador (el principal) | `prestador@demo.uy` | Martín Silva, "Cabalgatas del Polonio" (aprobado). Es el dueño de la cabalgata, así que con él se registra la asistencia de **UM-H0Y2Q8**. |
| Otros prestadores aprobados | `sierras@demo.uy`, `colonia@demo.uy`, `kayak@demo.uy` | Sierras de Minas Aventura, Sabores y Calles de Colonia y Punta Kayak. |
| Prestador pendiente | `pendiente@demo.uy` | Bodega Los Cerros: espera la aprobación del administrador y todavía no puede publicar. |

### Tarjetas de prueba (el pago es simulado)

| Tarjeta | Qué pasa |
|---|---|
| `4242 4242 4242 4242` | El pago se aprueba y la reserva queda confirmada. |
| `4000 0000 0000 0002` | El pago se rechaza y la reserva sigue pendiente. |
| `4000 0000 0000 0119` | El servicio de pagos no responde y la reserva no se confirma. |

El vencimiento y el CVV no se controlan; se pueden dejar vacíos o poner, por ejemplo, `12/30` y `123`.

## Antes de empezar

1. **Base de datos:**
   - Si usás **Supabase**, entrá al panel y fijate que el proyecto esté activo; se pausa después de una semana sin uso.
   - Si usás **Postgres local**, fijate que esté prendido.
2. **Datos de ejemplo al día:** las fechas de ejemplo se calculan desde el día en que se cargan (la reserva **UM-H0Y2Q8** es "de hoy"). Por defecto la app no borra nada, así que el mismo día de la demo, a la mañana:
   - poné `DB_DDL_AUTO=create` en el `.env` y levantá la app una vez (borra todo y carga los datos de nuevo);
   - bajala y volvé a `DB_DDL_AUTO=update` (el paso 3 la levanta de nuevo).

   Si te salteás esto, el paso de "Registrar asistencia" falla con "La asistencia se registra el día de la actividad".
3. **Levantá la app:** `./mvnw spring-boot:run`.
4. **Abrí dos ventanas del navegador**, una normal y una de incógnito. Así podés tener dos usuarios logueados al mismo tiempo (por ejemplo, turista y prestador).
5. **Internet:** si hay, se ve el pronóstico del clima en el detalle de cada actividad. Si no hay, todo funciona igual, sin el clima.

## 1. El visitante (sin cuenta): el portal

1. Entrá a http://localhost:8080. Mostrá la portada: el buscador y las próximas actividades.
2. **Buscá:** elegí "Destino: Lavalleja", o escribí "kayak". Mostrá los filtros de tipo, fecha y precio máximo.
3. **Abrí "Cabalgata al atardecer por las dunas"** y mostrá todo lo que tiene el detalle:
   - descripción, duración, edad mínima y política de cancelación;
   - equipamiento: qué da el prestador y qué hay que llevar;
   - **valoraciones** con estrellas, promedio y comentarios;
   - **próximas fechas**, con el pronóstico del clima de cada una.
4. **Apretá "Reservar":** como no hay sesión iniciada, el sistema pide ingresar.

## 2. El turista: reservar y pagar

1. Ingresá con **turista@demo.uy**. El sistema vuelve solo a la reserva.
2. Elegí 2 personas y escribí una observación, por ejemplo "Una persona es celíaca". Mostrá cómo se actualiza el total.
3. **Continuar al pago.** Mostrá el aviso de "tu lugar está guardado 2 horas".
4. **Pagá con la tarjeta `4000 0000 0000 0002`:** el pago se **rechaza** y la reserva sigue pendiente.
5. **Pagá con `4000 0000 0000 0119`:** el **servicio de pagos no responde**, y la reserva NO se confirma.
   - Esto es lo que pide la letra: manejar las fallas del servicio externo sin confirmar una reserva sin pago.
6. **Pagá con `4242 4242 4242 4242`:** se **aprueba**, y la reserva queda confirmada con su **código QR**.
   - Mostrá el historial de los 3 intentos de pago.
7. **Andá a "Mis reservas"** y mostrá los distintos estados:
   - confirmadas;
   - una realizada (Recorrido por el Barrio Histórico);
   - Kayak a la Isla Gorriti, que ya pasó: **valorala con estrellas y un comentario**;
   - la de la cabalgata dentro de unos días: **se puede cancelar**, porque falta más de 24 horas.

## 3. El prestador: su espacio de gestión

1. En la otra ventana, ingresá con **prestador@demo.uy**.
2. **"Mi emprendimiento":** el emprendimiento aprobado y sus actividades.
3. **Creá una actividad nueva:**
   - tipos, equipamiento (qué da el prestador y qué trae el turista), edad mínima, cierre de reservas y cancelación;
   - después **agregale una fecha**, con o sin cupo.
4. **"Reservas":** la lista de reservas de sus actividades, con las **observaciones** de cada turista.
5. **Registrar asistencia:** escribí el código **UM-H0Y2Q8** (la reserva de hoy). Pasa a "Realizada". Es lo que haría el escaneo del QR.
6. **(Opcional) Cancelar una fecha:** en el detalle de una actividad, "Cancelar fecha". Sus reservas se cancelan solas.

## 4. Un emprendimiento nuevo y el administrador

1. Cerrá sesión y entrá a **"Soy prestador"**. Registrá un emprendimiento nuevo.
2. Ingresá con ese prestador: aparece como **"Pendiente"** y no puede publicar actividades.
3. Ingresá con **admin@uymnatural.uy**:
   - la **bandeja de solicitudes** tiene el emprendimiento nuevo y Bodega Los Cerros;
   - **aprobá** uno y **rechazá** otro con un motivo. El prestador ve el motivo en su panel;
   - mostrá el **ABM de tipos de actividad y de equipamiento**: agregar, modificar y eliminar;
   - si intentás borrar un tipo que usa alguna actividad, el sistema no te deja.

## 5. La parte técnica

1. **API pública:** abrí http://localhost:8080/swagger-ui.html.
   - Probá "Top 3 del mes por departamento" o "Buscar actividades por palabra clave", con el botón *Try it out*.
2. **Tests:** corré `./mvnw test`. Hay más de 100 pruebas, todas en verde.
3. **Base de datos:** mostrá las tablas en Supabase (o en la consulta de Postgres) y compará con el MER.

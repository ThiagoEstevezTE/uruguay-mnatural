# Uruguay Mnatural

Plataforma que conecta turistas con prestadores de actividades turísticas de todo Uruguay.

Software desarrollado por **TeroBytes** para su cliente Uruguay Mnatural.
TeroBytes es la empresa de Patricio Saravia, Lorenzo Gianola, Federico Dolivo y Thiago Estevez (Proyecto TIC 2026, Universidad de Montevideo).

## Bajarlo de GitHub y correrlo (para el equipo)

**Guía completa, con cada paso y qué hacer si algo falla: [`docs/COMO_CORRERLO.md`](docs/COMO_CORRERLO.md).** Lo de abajo es el resumen.

Se hace una sola vez:

1. **Instalá dos cosas:**
   - **Docker Desktop** (https://www.docker.com/products/docker-desktop/). Trae la base de datos lista, así no hay que instalar PostgreSQL ni configurar nada.
   - **IntelliJ IDEA.** Si te pide el JDK, elegí *Download JDK* → versión **25**.
2. **Aceptá la invitación** al repositorio que te llega por mail desde GitHub.
3. **Bajá el proyecto.** En IntelliJ: **File → New → Project from Version Control**, pegá `https://github.com/ThiagoEstevezTE/uruguay-mnatural.git` y tocá **Clone**.

Cada vez que quieras usar la app:

1. **Abrí Docker Desktop** y esperá a que diga que está corriendo.
2. **Prendé la base.** En IntelliJ, abrí la pestaña **Terminal** (abajo) y escribí:

   ```
   docker compose up -d
   ```

3. **Levantá la app:** abrí `Proyectotic2026Application` y tocá el ▶ verde. O, en la misma terminal:

   ```
   ./mvnw spring-boot:run
   ```

4. **Abrí http://localhost:8080** y entrá con cualquiera de las [cuentas de prueba](#cuentas-de-prueba). Para saber cómo se usa cada pantalla, mirá [`docs/GUIA_DE_USO.md`](docs/GUIA_DE_USO.md).

No hace falta ningún archivo `.env`: sin él, la app se conecta sola a la base de Docker. La primera vez crea las tablas y carga los datos de prueba.

- Para apagar la base: `docker compose down` (los datos quedan guardados).
- Para traer lo último que subieron los demás: en IntelliJ, **Git → Pull**.
- Si `docker compose up -d` dice que el puerto 5432 está ocupado, es porque ya tenés un PostgreSQL instalado y prendido en tu compu. Apagalo, o usalo con un `.env` como dice la sección siguiente.

## Qué se necesita

- **JDK 25**.
- **Una base PostgreSQL**: la de Docker (`docker-compose.yml`), Supabase o un Postgres instalado en tu compu.
- Maven no hace falta: el proyecto trae el Maven Wrapper (`mvnw`).

## Cómo correrlo con otra base (Supabase o Postgres propio)

1. Copiá `.env.example` como `.env` y completá los datos de la base:

   ```
   DB_URL=jdbc:postgresql://HOST:PUERTO/BASE
   DB_USER=usuario
   DB_PASSWORD=contraseña
   ```

   - **Supabase:** en el proyecto, *Connect → JDBC → Session pooler*. Copiá la URL sin `user` ni `password`; esos dos datos van en `DB_USER` y `DB_PASSWORD`.
   - **Postgres propio:** `jdbc:postgresql://localhost:5432/uymnatural`, después de crear la base con `createdb uymnatural`.

2. Levantá la app con `./mvnw spring-boot:run` (o con el ▶ de IntelliJ) y abrí http://localhost:8080.

Por defecto la app **no borra nada** al arrancar (`DB_DDL_AUTO=update`): crea las tablas que falten y carga los datos de ejemplo solo si la base está vacía. Es lo que hay que usar con Supabase.

Para que la demo local empiece siempre igual, poné `DB_DDL_AUTO=create` en tu `.env`: así cada arranque borra todo y vuelve a cargar los datos de ejemplo.

Con Supabase, usá `create` **una sola vez y a propósito**: la mañana de la demo, porque las fechas de ejemplo se calculan desde el día en que se cargan (por ejemplo, la reserva "de hoy" del guion). Después volvé a `update`, así un reinicio no borra lo que se hizo. Los pasos están en `docs/GUION_DEMO.md`.

## Cuentas de prueba

Todas usan la contraseña `demo1234`. Se puede cambiar con `DEMO_PASSWORD` en el `.env`, antes del primer arranque; conviene hacerlo si la base o la app quedan accesibles para otros.

| Rol | Email | Qué tiene cargado |
|---|---|---|
| Administrador | admin@uymnatural.uy | Una solicitud pendiente para aprobar |
| Turista | turista@demo.uy | Reservas confirmadas (una es hoy), una realizada y una para valorar |
| Prestador aprobado | prestador@demo.uy | Cabalgatas del Polonio, con reservas y valoraciones |
| Prestador aprobado | sierras@demo.uy, colonia@demo.uy, kayak@demo.uy | Otros emprendimientos con actividades |
| Prestador pendiente | pendiente@demo.uy | Bodega Los Cerros, esperando aprobación |

## Tarjetas de prueba

Los pagos van por **Mercado Pago en modo prueba**: no se cobra plata de verdad. Para que ande, el `.env` tiene que tener `MP_ACCESS_TOKEN` y `MP_PUBLIC_KEY` (las claves se pasan por privado, nunca se suben a GitHub).

| Dato | Valor |
|---|---|
| Número | `5031 7557 3453 0604` |
| Vencimiento y código | `11/30` y `123` |
| Documento | CI `12345672` |
| Email | `comprador@test.uy` |
| Nombre del titular | `APRO`: pago aprobado, la reserva se confirma. `OTHE`: pago rechazado, la reserva sigue pendiente |

Sin claves de Mercado Pago, poné `PAYMENT_PROVIDER=simulator` en el `.env` y se usa el **servicio de pagos simulado** que trae la app (`simulator/FakePaymentGatewayController`):

| Tarjeta | Resultado |
|---|---|
| `4242 4242 4242 4242` | Pago aprobado: la reserva se confirma |
| `4000 0000 0000 0002` | Pago rechazado: la reserva sigue pendiente |
| `4000 0000 0000 0119` | El servicio no responde: la reserva sigue pendiente |

## Tests

```
./mvnw test
```

Los tests usan H2 en memoria, así que no necesitan Postgres.

## Documentación

- [`docs/COMO_CORRERLO.md`](docs/COMO_CORRERLO.md): cómo bajar el proyecto y hacerlo andar en tu compu, paso a paso.
- [`docs/GUIA_DE_USO.md`](docs/GUIA_DE_USO.md): cómo se usa cada función de la página, paso a paso y con capturas.
- [`docs/GUION_DEMO.md`](docs/GUION_DEMO.md): el recorrido de la presentación y cómo prepararla.
- `docs/manuales/`: manuales de usuario para turistas, prestadores y administradores.
- `docs/GUIA_DEL_CODIGO.md`: cómo está hecho el código, parte por parte, y los conceptos clave.
- `docs/requerimientos/`: letra del proyecto, actas con el cliente y el documento de requerimientos y casos de uso.
- `docs/diagramas/`: MER y modelo relacional.

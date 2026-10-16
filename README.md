# Uruguay Mnatural

Plataforma que conecta turistas con prestadores de actividades turísticas de todo Uruguay.

Software desarrollado por **TeroBytes** para su cliente Uruguay Mnatural.
TeroBytes es la empresa de Patricio Saravia, Lorenzo Gianola, Federico Dolivo y Thiago Estevez (Proyecto TIC 2026, Universidad de Montevideo).

## Qué se necesita

- **JDK 25**.
- **Una base PostgreSQL**: Supabase o un Postgres local.
- Maven no hace falta: el proyecto trae el Maven Wrapper (`mvnw`).

## Cómo correrlo

1. Copiá `.env.example` como `.env` y completá los datos de la base:

   ```
   DB_URL=jdbc:postgresql://HOST:PUERTO/BASE
   DB_USER=usuario
   DB_PASSWORD=contraseña
   ```

   - **Supabase:** en el proyecto, *Connect → JDBC → Session pooler*. Copiá la URL sin `user` ni `password`; esos dos datos van en `DB_USER` y `DB_PASSWORD`.
   - **Postgres local:** `jdbc:postgresql://localhost:5432/uymnatural`, después de crear la base con `createdb uymnatural`.

2. Levantá la app:

   ```
   ./mvnw spring-boot:run
   ```

   En IntelliJ también se puede correr `Proyectotic2026Application`.

3. Abrí http://localhost:8080.

Por defecto la app **no borra nada** al arrancar (`DB_DDL_AUTO=update`): crea las tablas que falten y carga los datos de ejemplo solo si la base está vacía. Es lo que hay que usar con Supabase.

Para que la demo local empiece siempre igual, poné `DB_DDL_AUTO=create` en tu `.env`: así cada arranque borra todo y vuelve a cargar los datos de ejemplo. Nunca uses `create` con Supabase.

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

El pago lo procesa un **servicio de pagos externo simulado** (`simulator/FakePaymentGatewayController`). La app lo llama por HTTP, igual que llamaría a uno real.

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

- `docs/GUIA_DEL_CODIGO.md`: cómo está hecho el código, parte por parte, y los conceptos clave.
- `docs/HISTORIAL.md`: decisiones tomadas y equivalencia entre el MER y el código.
- `docs/PLAN.md`: plan original del registro de prestadores.
- `docs/requerimientos/`: letra del proyecto y actas con el cliente.
- `docs/diagramas/`: MER y modelo relacional.

# Uruguay Mnatural

Plataforma que conecta turistas con prestadores de actividades turísticas de todo Uruguay.
Proyecto TIC 2026 (Universidad de Montevideo), equipo **TeroBytes**: Patricio Saravia, Lorenzo Gianola, Federico Dolivo y Thiago Estevez.

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

Cada vez que arranca, la app crea las tablas de cero y carga los datos de ejemplo (`spring.jpa.hibernate.ddl-auto=create`). Así la demo siempre empieza igual.

## Cuentas de prueba

Todas usan la contraseña `demo1234`.

| Rol | Email |
|---|---|
| Administrador | admin@uymnatural.uy |
| Turista | turista@demo.uy |
| Prestador aprobado | prestador@demo.uy |
| Prestador pendiente de aprobación | pendiente@demo.uy |

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

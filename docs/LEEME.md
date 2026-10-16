# Para Thiago: cómo usar este paquete

Este zip tiene el proyecto **Uruguay Mnatural** (Proyecto TIC 2026, TeroBytes) tal como lo dejó Lorenzo, más todo el contexto necesario para que tu Claude Code lo entienda y te ayude a seguir.

## Pasos

1. Descomprimí el zip en cualquier carpeta (por ejemplo, el Escritorio).
2. Abrí Claude Code **dentro de la carpeta descomprimida** `proyectotic2026-handoff`.
3. Escribile:

   > Leé `PROMPT_PARA_CLAUDE.md` y seguí sus instrucciones.

Tu Claude va a:
- revisar qué tenés instalado,
- preguntarte dónde querés dejar el proyecto,
- dejarlo compilando y corriendo,
- explicarte en qué estado está.

**No va a programar nada nuevo hasta que vos se lo pidas.** Vos decidís el ritmo: si querés ir paso a paso escribiendo vos el código (como hizo Lorenzo), o si preferís que él escriba.

## Qué hay adentro

```
proyectotic2026-handoff/
├── LEEME.md                  ← este archivo
├── PROMPT_PARA_CLAUDE.md     ← instrucciones para tu Claude
├── proyectotic2026/          ← EL PROYECTO (Spring Boot)
│   ├── CLAUDE.md             ← contexto permanente para Claude
│   ├── docs/
│   │   ├── HISTORIAL.md      ← qué se hizo, cómo y por qué
│   │   ├── PLAN.md           ← qué falta y cómo se pensaba seguir
│   │   ├── requerimientos/   ← letra del proyecto + actas (PDF y TXT)
│   │   └── diagramas/        ← MER y modelo relacional
│   ├── src/ ...
│   └── pom.xml
└── ejemplos-moodle/
    └── demospring_thymeleaf/ ← ejemplo del curso que se usó como guía de estilo
```

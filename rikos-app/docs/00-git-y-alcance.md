# 00 · Git y alcance de cambios

**Regla: todo lo que se haga en la app (código, docs, commits y push) afecta únicamente la carpeta `rikos-app/`.** No se toca `rikos-backend/`, `rikos-web/`, `docs/` (la general de la raíz), `CLAUDE.md`, `README.md`, `ESTADO.md` ni nada fuera de `rikos-app/`.

El repositorio es uno solo (`D:\Proyectos\RIKOS`, con `rikos-backend`, `rikos-web` y `rikos-app` como carpetas hermanas), por eso hace falta disciplina al usar git.

## Cómo se hace

1. Antes de empezar, `git status` en la raíz. Si hay cambios ajenos a la app (de web o backend), se dejan quietos.
2. Agregar **solo** la carpeta de la app:
   ```
   git add rikos-app/
   ```
   Nunca `git add .`, `git add -A` ni `git commit -a` desde la raíz: arrastran cambios de otros equipos.
3. Confirmar lo que va a entrar: `git diff --cached --stat`. Todas las rutas deben empezar con `rikos-app/`.
4. Commit limitado a la carpeta:
   ```
   git commit -m "app: <qué cambió>" -- rikos-app/
   ```
   Convención sugerida: prefijo `app:` en el mensaje, para distinguirlo en el historial común.
5. Push de la rama. Se recomienda trabajar en una rama propia (`app/<tema>`) y no empujar directo a la rama principal.

## Qué NO hace la app

- No edita el backlog ni sus enmiendas (`docs/producto/prd/`): el cruce con el backlog vive en `rikos-app/docs/03-pantallas-vs-backlog.md`.
- No agrega migraciones, endpoints ni cambios al backend. Lo que falte de la API se anota en `docs/02-brechas-de-api.md`.
- No modifica la documentación general del repo. Si algo de la app debe quedar allí, lo decide el equipo, no se hace como efecto secundario.
- No toca archivos `.env` ni credenciales.

## Regla de trabajo

No se hace commit ni push sin que el equipo lo pida (igual que en el resto del repo).

## Verificación rápida antes del push

```
git diff --name-only origin/<rama-base>...HEAD | grep -v '^rikos-app/'
```
Si imprime algo, hay archivos fuera de la app en el cambio y no se debe empujar hasta revisar.

# Sesion

Aqui vive el usuario activo, sus permisos efectivos y el cambio rapido
por PIN entre mozos.

Concentrarlo en un solo lugar es lo que permite que un componente oculte
botones segun el permiso (RN-012: un usuario sin permiso no puede anular,
descontar ni ver costos) sin repetir esa comprobacion en cada pantalla.

`SesionContexto.tsx` (US-002/US-003, Sprint 1) ya guarda el usuario activo, sus
roles y el local activo, con `useSesion()` como unico punto de lectura.

**ADM-29 parcial:** la pantalla de usuarios ya asigna rol y locales, pero las
excepciones de permiso por usuario individual y el componente que oculta
acciones segun permiso llegan con US-004 en el Sprint 3.

-- V15 sembro unidad_medida y categoria_producto con ids escritos a mano
-- de 25 caracteres, no 26 -- rompe la convencion de ULID de longitud fija
-- del proyecto (2.1) y Postgres los guarda con relleno de espacio en el
-- CHAR(26), lo que ensucia cualquier respuesta JSON que use ese id como
-- FK (categoriaId/unidadBaseId en ProductoRespuesta, por ejemplo).
--
-- No se edita V15 (ya aplicada, Flyway es autoritativo) -- se corrige con
-- un UPDATE aqui. Se inserta un caracter de relleno extra justo despues
-- del prefijo de 9 caracteres tipo-ULID, sin tocar el sufijo significativo
-- de cada id.
--
-- Postgres valida una FK sin DEFERRABLE al final de cada sentencia, no al
-- final de la fila ni de la transaccion. Para la autorreferencia de
-- categoria_producto eso se resuelve actualizando id y categoria_padre_id
-- en una sola sentencia UPDATE (ambos lados cambian a la vez, quedan
-- consistentes al terminar esa sentencia). Para producto -> unidad_medida/
-- categoria_producto (tablas distintas, sentencias distintas por fuerza)
-- no hay ese atajo; en este punto del desarrollo la unica fila real en
-- producto es un dato de prueba de esta sesion (codigo ARROZ-001), asi
-- que se borra antes de tocar los ids en vez de manejar el reordenamiento
-- entre tablas -- no hay nada de negocio real que preservar todavia.

DELETE FROM producto WHERE codigo = 'ARROZ-001';

UPDATE unidad_medida
    SET id = substring(id from 1 for 9) || '0' || substring(id from 10)
    WHERE length(id) = 25;

UPDATE categoria_producto SET
    id = substring(id from 1 for 9) || '0' || substring(id from 10),
    categoria_padre_id = CASE WHEN categoria_padre_id IS NOT NULL
        THEN substring(categoria_padre_id from 1 for 9) || '0' || substring(categoria_padre_id from 10)
        ELSE NULL END
    WHERE length(id) = 25;

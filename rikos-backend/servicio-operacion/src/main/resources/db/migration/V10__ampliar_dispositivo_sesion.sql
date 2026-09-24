-- El modelo (3.10) definio sesion.dispositivo como texto(120), pensado
-- para un identificador corto. El valor real que se guarda ahi es el
-- User-Agent que manda el navegador, que en Chrome/Edge actuales supera
-- facilmente los 120 caracteres -- el INSERT fallaba con "value too long"
-- en cuanto se probo desde un navegador real, no desde curl/PowerShell.
-- Se amplia a 255 sin cambiar el significado de la columna.

ALTER TABLE sesion ALTER COLUMN dispositivo TYPE VARCHAR(255);

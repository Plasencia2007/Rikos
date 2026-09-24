-- Una instancia de Postgres, una base por servicio.
-- Ningun servicio consulta la base del otro: se hablan por eventos.
CREATE DATABASE rikos_operacion   OWNER rikos;
CREATE DATABASE rikos_facturacion OWNER rikos;

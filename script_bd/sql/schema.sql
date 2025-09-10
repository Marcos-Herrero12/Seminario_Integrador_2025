-- Elimina la base de datos si ya existe para empezar de cero
DROP DATABASE IF EXISTS db_pasantias;
CREATE DATABASE db_pasantias;
USE db_pasantias;

--
-- Estructura de la tabla `Empresa`
--
CREATE TABLE Empresa (
    id_empresa INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    ciudad VARCHAR(100),
    direccion TEXT,
    email_contacto VARCHAR(255),
    modalidad VARCHAR(50)
);

--
-- Estructura de la tabla `Contacto`
--
CREATE TABLE Contacto (
    id_contacto INT AUTO_INCREMENT PRIMARY KEY,
    id_empresa INT,
    nombre_completo VARCHAR(255) NOT NULL,
    horario_entrevista TEXT,
    FOREIGN KEY (id_empresa) REFERENCES Empresa(id_empresa) ON DELETE CASCADE
);

--
-- Inserta datos de ejemplo
--
INSERT INTO Empresa (nombre, ciudad, direccion, email_contacto, modalidad) VALUES
('BIOFARMA S.A', 'Córdoba', 'Bv. de los Polacos 6446 Barrio Los Boulevares', 'rrhh@biofarmaweb.com.ar', 'Presencial'),
('INDACOR S.A.', 'JUAREZ CELMAN', 'Ruta 9 norte km 721 – Juárez Celman', 'aracelipenaflor@pollosindacor.com.ar', 'Presencial');

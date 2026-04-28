# DS Racing Garage

> Simulador de garaje personal — construye tu build, prueba tu coche en el banco de potencia y descarga los datos.

---

## ¿Qué es esto?

DS Racing Garage es una aplicación de escritorio desarrollada en Java que simula la gestión de un garaje de competición. Puedes crear una cuenta, añadir coches de diferentes marcas y modelos, modificarlos con piezas de rendimiento y someterlos a una prueba en un dinamómetro virtual animado que muestra en tiempo real cómo responde el motor según las RPM.

---

## Características principales

**Gestión de usuarios y sesión**
- Registro e inicio de sesión con usuario y contraseña
- Cada usuario tiene su propio garaje persistente — los datos se guardan entre sesiones gracias a H2 en modo fichero
- Múltiples coches por usuario, cada uno con su propia configuración de piezas

**Selección de coches**
Los coches disponibles como punto de partida son:
- Nissan S13 (1992) — 200 HP · 250 Nm · 1200 kg
- Honda Civic Type R (2001) — 185 HP · 190 Nm · 1100 kg
- Subaru Impreza WRX (2003) — 230 HP · 310 Nm · 1400 kg
- Citroën C4 Coupé (2025) — 150 HP · 250 Nm · 1230 kg
- BMW E39 (1999) — 193 HP · 200 Nm · 1500 kg

**Editor de piezas**
Cada coche tiene 8 slots de modificación independientes. Cada pieza instalada afecta directamente a los valores simulados en el dinamómetro:

| Slot | Ejemplos de piezas |
|---|---|
| Turbo | Stage 1 / Stage 2 / Stage 3 |
| Escape | Sport / Racing |
| ECU / Admisión | Stage 1 / Stage 2 |
| Suspensión | Sport / Rally / Drift |
| Neumáticos | Sport / Drift / Rally |
| Diferencial | Deportivo / Racing |
| Frenos | Sport / Racing |
| Aerodinámica | Alerón trasero / Kit completo |

El editor muestra en tiempo real la comparativa entre los valores base del coche de serie y los valores con las piezas instaladas (HP, Nm, masa y grip).

**Dinamómetro animado**
La prueba dyno simula una pasada completa de RPM con animación en tiempo real:
- Curva de potencia (HP) en rojo dibujándose progresivamente
- Curva de torque (Nm) en azul cian
- Cuenta-RPM con aguja animada que cambia de color verde → amarillo → rojo según la zona de trabajo
- Indicadores numéricos de HP, Nm y RPM actualizándose en vivo
- Anotaciones de pico al finalizar la prueba

**Exportación CSV**
Al finalizar la prueba dyno se puede descargar un CSV con:
- Comparativa completa serie vs modificado (HP, Nm, masa, grip con sus deltas)
- Lista de piezas instaladas con sus valores individuales
- Curva completa RPM → HP y Nm punto a punto

---

## Stack técnico

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 22 | Lenguaje principal |
| Spring Boot | 3.2.4 | Inyección de dependencias, JPA, ciclo de vida |
| JavaFX | 21.0.2 | Interfaz gráfica |
| Hibernate | 6.4.4 | ORM y gestión de entidades |
| H2 Database | 2.2.224 | Base de datos embebida en fichero |
| Maven | — | Gestión de dependencias y build |

---

## Estructura del proyecto

```
com.dsracing.garage
├── config/
│   ├── DataLoader.java          # Carga de datos iniciales al arrancar
│   └── SimulationRunner.java    # Simulación de consola (desarrollo)
├── model/entity/
│   ├── User, Garage, Car, Part  # Entidades JPA principales
│   ├── DynoResult, DriftRun     # Resultados de pruebas
│   ├── TelemetrySample          # Datos de telemetría tick a tick
│   └── BuildHistory             # Historial de builds por usuario
├── repository/                  # Repositorios Spring Data JPA
├── service/                     # Lógica de negocio
│   └── impl/
│       ├── DynoService.java     # Cálculo de curvas de potencia y torque
│       └── DriftSimulator.java  # Simulación física de tramo de drift
├── ui/fx/controllers/
│   ├── LoginController.java     # Pantalla de login y garaje
│   ├── DynoController.java      # Ventana del dinamómetro animado
│   └── PartEditorController.java# Editor de piezas con catálogo
├── util/csv/
│   └── CsvExporter.java         # Exportación de resultados a CSV
├── GarageApplication.java       # Arranque JavaFX + Spring Boot
└── Launcher.java                # Entry point separado (requerido por fat JAR)
```

---

## Cómo ejecutar

**Requisitos:**
- Java 17 o superior
- Maven 3.8+

**Clonar y ejecutar:**
```bash
git clone https://github.com/tu-usuario/ds-racing-garage.git
cd ds-racing-garage
mvn clean javafx:run
```

**O compilar y ejecutar el JAR:**
```bash
mvn clean package
java -jar target/garage-1.0.0.jar
```

Los datos se guardan automáticamente en `~/.dsracing/garage.mv.db` al cerrar la aplicación.

---

## Capturas de pantalla
<img width="425" height="476" alt="image" src="https://github.com/user-attachments/assets/30b06afa-1d52-4cdb-a5b4-bba49b656f0c" />
<img width="1837" height="355" alt="image" src="https://github.com/user-attachments/assets/321b8b14-f34b-44a5-ac99-58038ab4d536" />
<img width="1837" height="841" alt="image" src="https://github.com/user-attachments/assets/4d00be77-aabc-4e6d-8412-b6ca584ce612" />
<img width="917" height="598" alt="image" src="https://github.com/user-attachments/assets/a360dca9-d0a4-4746-aed4-77ff3077712c" />
<img width="954" height="529" alt="image" src="https://github.com/user-attachments/assets/445f7216-0a84-4e0c-84a1-1a9633eb3bc8" />

---

## Autor

Desarrollado por **Alberto Martí** como proyecto final.

---
# IMPORTANTE
Este proyecto ha sido realizado como trabajo de final de módulo, pero como se puede ver, hay algunos archivos extra que no se usan de manera visible.
Esto se debe a que tengo la intención de implementar más funcionalidades en un futuro, usando mecánicas realistas para que se pueda usar el programa con intenciones reales y 
así poder realizar estimaciones del rendimiento de cada automóvil tras modificarlo físicamente.
---
## Licencia

Este proyecto es de uso educativo y personal.

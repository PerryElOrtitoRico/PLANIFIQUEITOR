# Planificador de Estudio Desktop (Java)

Aplicación nativa de escritorio desarrollada en **Java (Swing + FlatLaf)** para la gestión completa de plan de estudio universitario, materias, guía semanal y fechas importantes.

## 🚀 Requisitos
- Java JDK 17 o superior.
- Maven 3.6+.

## 🛠️ Cómo Compilar y Ejecutar

### Opción 1: Ejecutar directamente con Maven
```bash
mvn clean compile exec:java -Dexec.mainClass="com.planificador.Main"
```

### Opción 2: Generar JAR Ejecutable y ejecutar
```bash
mvn clean package
java -jar target/planificador-estudio-desktop-1.0.0-jar-with-dependencies.jar
```

## 📂 Estructura del Proyecto
- `com.planificador.Main`: Punto de entrada de la aplicación.
- `com.planificador.model`: Modelos de datos (Subject, Unit, Topic, StudySession, ImportantDate).
- `com.planificador.service.DataManager`: Gestor de almacenamiento en JSON (persistente en disco).
- `com.planificador.ui`: Interfaces gráficas nativas Java Swing (MainWindow, TodayPanel, WeeklyPanel, SubjectsPanel, DatesPanel, StatsPanel, TimerDialog).

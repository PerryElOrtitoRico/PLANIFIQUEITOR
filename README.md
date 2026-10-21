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

## 💾 Dónde se guardan los datos
Los datos viven siempre en el mismo archivo, sin importar desde qué carpeta se abra la app:

```
~/.local/share/planificador-estudio/planificador_datos.json
```

(o `$XDG_DATA_HOME/planificador-estudio/` si esa variable está definida). Se guarda primero en un temporal y después se reemplaza, así un corte a mitad de escritura no deja el archivo roto. Si el archivo no se puede leer, se guarda una copia `planificador_datos.json.danado-<fecha>` antes de cargar los datos de ejemplo.

Las versiones anteriores guardaban `planificador_datos.json` en la carpeta desde donde se abría la app. Al primer inicio, si la ubicación nueva no existe, se **copia** el más reciente de esos archivos viejos (carpeta actual o carpeta personal); el original no se borra.

## 🧪 Tests
```bash
mvn test
```

## 📂 Estructura del Proyecto
- `com.planificador.Main`: Punto de entrada de la aplicación.
- `com.planificador.model`: Modelos de datos (Subject, Unit, Topic, StudySession, ImportantDate).
- `com.planificador.service.DataManager`: Gestor de almacenamiento en JSON (persistente en disco).
- `com.planificador.ui`: Interfaces gráficas nativas Java Swing (MainWindow, TodayPanel, WeeklyPanel, SubjectsPanel, DatesPanel, StatsPanel, TimerDialog).

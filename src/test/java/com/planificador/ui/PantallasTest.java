package com.planificador.ui;

import com.planificador.model.AttendanceRecord;
import com.planificador.model.CourseRules;
import com.planificador.model.Evaluation;
import com.planificador.service.DataManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de humo de la interfaz sin pantalla: arma cada panel con datos inventados y lo dibuja en una imagen.
 * Con -Dcapturas=<carpeta> guarda las imágenes para mirarlas.
 */
class PantallasTest {

    @TempDir
    Path tmp;

    @Test
    void todosLosPanelesSeArmanYSeDibujan() throws Exception {
        DataManager dm = UiDatos.cargar(tmp);
        var curso = dm.getSubjects().get(0).getOrCreateCourse();
        curso.setRules(new CourseRules(75, 6, 8, 2, List.of(true, true), 16));
        curso.getEvaluations().add(new Evaluation("e1", Evaluation.PARCIAL, 1, false, 8.5, "2026-09-20"));
        curso.getEvaluations().add(new Evaluation("e2", Evaluation.TP, 1, false, 9, null));
        for (int i = 0; i < 9; i++) curso.getAttendance().add(new AttendanceRecord("2026-08-" + (10 + i), i != 3));

        List<JComponent> paneles = List.of(new TodayPanel(), new WeeklyPanel(), new SubjectsPanel(), new DatesPanel(),
                new StatsPanel(), new StudyTimePanel(null), new CoursePanel(null), new PomodoroPanel(null),
                new CourseRulesForm(curso.getRules()), new EvaluationForm());
        String carpeta = System.getProperty("capturas");
        for (JComponent p : paneles) {
            BufferedImage img = dibujar(p, p instanceof PomodoroPanel || p instanceof CourseRulesForm
                    || p instanceof EvaluationForm ? new Dimension(440, 340) : new Dimension(880, 700));
            assertTrue(img.getWidth() > 0);
            if (carpeta != null) {
                Files.createDirectories(Path.of(carpeta));
                ImageIO.write(img, "png", Path.of(carpeta, p.getClass().getSimpleName() + ".png").toFile());
            }
        }
    }

    static BufferedImage dibujar(JComponent c, Dimension d) {
        c.setSize(d);
        c.setPreferredSize(d);
        c.doLayout();
        layoutTodo(c);
        BufferedImage img = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, d.width, d.height);
        c.printAll(g);
        g.dispose();
        return img;
    }

    static void layoutTodo(Container c) {
        c.doLayout();
        for (Component h : c.getComponents()) if (h instanceof Container k) layoutTodo(k);
    }
}

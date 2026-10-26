package com.planificador.ui;

import com.planificador.model.CourseRules;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Formulario de las reglas de cursada de una materia (se muestra con JOptionPane). */
public class CourseRulesForm extends JPanel {

    final JSpinner minAttendance;
    final JSpinner passGrade;
    final JSpinner promotionGrade;
    final JSpinner examCount;
    final JSpinner plannedClasses;
    final List<JCheckBox> retakeBoxes = new ArrayList<>();
    private final JPanel retakesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));

    public CourseRulesForm(CourseRules rules) {
        minAttendance = new JSpinner(new SpinnerNumberModel(rules.getMinAttendancePercent(), 0.0, 100.0, 5.0));
        passGrade = new JSpinner(new SpinnerNumberModel(rules.getPassGrade(), 0.0, 10.0, 0.5));
        promotionGrade = new JSpinner(new SpinnerNumberModel(rules.getPromotionGrade(), 0.0, 10.0, 0.5));
        examCount = new JSpinner(new SpinnerNumberModel(rules.getExamCount(), 0, 10, 1));
        plannedClasses = new JSpinner(new SpinnerNumberModel(rules.getPlannedClasses(), 0, 200, 1));

        setLayout(new GridLayout(0, 2, 8, 6));
        add(new JLabel("Asistencia mínima (%):"));
        add(minAttendance);
        add(new JLabel("Nota para regularizar:"));
        add(passGrade);
        add(new JLabel("Nota para promocionar (0 = no hay):"));
        add(promotionGrade);
        add(new JLabel("Cantidad de parciales:"));
        add(examCount);
        add(new JLabel("Con recuperatorio:"));
        add(retakesPanel);
        add(new JLabel("Clases del cuatrimestre (0 = no sé):"));
        add(plannedClasses);

        buildRetakeBoxes(rules.getExamCount(), rules.getRetakes());
        examCount.addChangeListener(e -> buildRetakeBoxes((Integer) examCount.getValue(), currentRetakes()));
    }

    /** Una casilla por parcial ("P1", "P2", …), marcada según previous (las que faltan, marcadas). */
    private void buildRetakeBoxes(int n, List<Boolean> previous) {
        retakeBoxes.clear();
        retakesPanel.removeAll();
        for (int i = 0; i < n; i++) {
            JCheckBox box = new JCheckBox("P" + (i + 1), i >= previous.size() || Boolean.TRUE.equals(previous.get(i)));
            retakeBoxes.add(box);
            retakesPanel.add(box);
        }
        retakesPanel.revalidate();
        retakesPanel.repaint();
    }

    private List<Boolean> currentRetakes() {
        List<Boolean> out = new ArrayList<>();
        for (JCheckBox b : retakeBoxes) out.add(b.isSelected());
        return out;
    }

    /** Las reglas que quedaron cargadas en el formulario. */
    public CourseRules toRules() {
        // TODO T09a
        throw new UnsupportedOperationException();
    }
}

package com.planificador.ui;

import com.planificador.model.Evaluation;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Formulario para cargar una nota de parcial, recuperatorio o TP (se muestra con JOptionPane). */
public class EvaluationForm extends JPanel {

    final JComboBox<String> kindCombo = new JComboBox<>(new String[]{"Parcial", "TP"});
    final JSpinner numberSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
    final JCheckBox retakeCheck = new JCheckBox("Es el recuperatorio");
    final JSpinner gradeSpinner = new JSpinner(new SpinnerNumberModel(6.0, 0.0, 10.0, 0.25));
    final JTextField dateField = new JTextField(LocalDate.now().toString());

    public EvaluationForm() {
        setLayout(new GridLayout(0, 2, 8, 6));
        add(new JLabel("Tipo:"));
        add(kindCombo);
        add(new JLabel("Número:"));
        add(numberSpinner);
        add(new JLabel(""));
        add(retakeCheck);
        add(new JLabel("Nota:"));
        add(gradeSpinner);
        add(new JLabel("Fecha (AAAA-MM-DD, opcional):"));
        add(dateField);
        kindCombo.addActionListener(e -> retakeCheck.setEnabled(kindCombo.getSelectedIndex() == 0));
    }

    /**
     * La nota cargada en el formulario.
     * @throws IllegalArgumentException si la fecha no está vacía y no es AAAA-MM-DD
     */
    public Evaluation toEvaluation(String id) {
        // TODO T09a
        throw new UnsupportedOperationException();
    }
}

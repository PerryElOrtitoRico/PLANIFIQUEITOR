package com.planificador.app.ui;

import android.content.Context;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.planificador.app.logica.FormularioAjustes;
import com.planificador.model.PomodoroSettings;
import com.planificador.service.DataManager;

/** Ajustes del Pomodoro. Rigen desde la próxima fase; la que está corriendo conserva su duración. */
public class AjustesFragment extends PantallaBase {

    @Override
    void dibujar(LinearLayout raiz) {
        Context c = requireContext();
        PomodoroSettings a = DataManager.getInstance().getPomodoroSettings();
        raiz.addView(Vistas.titulo(c, "Ajustes del Pomodoro"));
        EditText estudio = Vistas.campo(c, "", String.valueOf(a.getWorkMinutes()), true);
        EditText corto = Vistas.campo(c, "", String.valueOf(a.getShortBreakMinutes()), true);
        EditText largo = Vistas.campo(c, "", String.valueOf(a.getLongBreakMinutes()), true);
        EditText cada = Vistas.campo(c, "", String.valueOf(a.getCyclesBeforeLongBreak()), true);
        raiz.addView(Vistas.etiquetado(c, "Estudio (minutos)", estudio));
        raiz.addView(Vistas.etiquetado(c, "Descanso corto (minutos)", corto));
        raiz.addView(Vistas.etiquetado(c, "Descanso largo (minutos)", largo));
        raiz.addView(Vistas.etiquetado(c, "Descanso largo cada (ciclos)", cada));
        raiz.addView(Vistas.texto(c, "Los cambios rigen desde la próxima fase y llegan a la PC por Syncthing."));
        raiz.addView(Vistas.boton(c, "Guardar", () -> {
            try {
                DataManager.getInstance().setPomodoroSettings(FormularioAjustes.ajustes(estudio.getText().toString(),
                        corto.getText().toString(), largo.getText().toString(), cada.getText().toString()));
                android.widget.Toast.makeText(c, "Ajustes guardados", android.widget.Toast.LENGTH_SHORT).show();
                refrescar();
            } catch (IllegalArgumentException ex) {
                Vistas.aviso(c, ex.getMessage());
            }
        }));
    }
}

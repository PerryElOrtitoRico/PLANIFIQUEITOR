package com.planificador.app.ui;

import android.widget.LinearLayout;

/** Se completa en las tareas siguientes de la etapa 3. */
public class AjustesFragment extends PantallaBase {
    @Override
    void dibujar(LinearLayout raiz) {
        raiz.addView(Vistas.titulo(requireContext(), "Ajustes"));
    }
}

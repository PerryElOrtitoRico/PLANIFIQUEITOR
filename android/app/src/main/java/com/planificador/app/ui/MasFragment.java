package com.planificador.app.ui;

import android.content.Context;
import android.widget.LinearLayout;

/** Lista de las demás pantallas; cada una se abre encima, con la flecha atrás para volver. */
public class MasFragment extends PantallaBase {

    @Override
    void dibujar(LinearLayout raiz) {
        Context c = requireContext();
        MainActivity a = (MainActivity) requireActivity();
        raiz.addView(Vistas.titulo(c, "Más"));
        raiz.addView(Vistas.boton(c, "Materias", () -> a.abrir(new MateriasFragment())));
        raiz.addView(Vistas.boton(c, "Fechas clave", () -> a.abrir(new FechasFragment())));
        raiz.addView(Vistas.boton(c, "Progreso", () -> a.abrir(new ProgresoFragment())));
        raiz.addView(Vistas.boton(c, "Tiempo", () -> a.abrir(new TiempoFragment())));
        raiz.addView(Vistas.boton(c, "Cursada", () -> a.abrir(new CursadaFragment())));
        raiz.addView(Vistas.boton(c, "Ajustes del Pomodoro", () -> a.abrir(new AjustesFragment())));
    }
}

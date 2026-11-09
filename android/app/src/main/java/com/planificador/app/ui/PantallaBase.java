package com.planificador.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.planificador.app.datos.Datos;

/**
 * Pantalla armada por código que se redibuja entera desde DataManager en cada onResume y después de cada cambio,
 * así nunca trabaja con objetos de antes de una recarga.
 */
abstract class PantallaBase extends Fragment {

    /** Argumento: la pantalla se abrió desde Más y muestra "← Volver". */
    static final String CON_VOLVER = "conVolver";

    LinearLayout raiz;

    @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup parent, Bundle b) {
        ScrollView sv = new ScrollView(requireContext());
        raiz = Vistas.columna(requireContext());
        int p = Vistas.dp(requireContext(), 16);
        raiz.setPadding(p, p / 2, p, p);
        sv.addView(raiz);
        return sv;
    }

    @Override
    public void onResume() {
        super.onResume();
        refrescar();
    }

    void refrescar() {
        if (!isAdded() || raiz == null || !Datos.asegurar(requireContext())) return;
        raiz.removeAllViews();
        if (getArguments() != null && getArguments().getBoolean(CON_VOLVER)) {
            raiz.addView(Vistas.boton(requireContext(), "← Volver",
                    () -> requireActivity().getOnBackPressedDispatcher().onBackPressed()));
        }
        dibujar(raiz);
    }

    abstract void dibujar(LinearLayout raiz);
}

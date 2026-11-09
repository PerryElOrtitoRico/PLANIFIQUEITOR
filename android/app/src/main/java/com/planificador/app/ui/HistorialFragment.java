package com.planificador.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/** Historial: se completa en la Tarea 5. */
public class HistorialFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup parent, Bundle b) {
        TextView t = new TextView(requireContext());
        t.setPadding(48, 48, 48, 48);
        t.setText("Historial");
        return t;
    }
}

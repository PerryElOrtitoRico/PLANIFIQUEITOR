package com.planificador.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.planificador.app.R;
import com.planificador.app.datos.Datos;
import com.planificador.app.datos.Rutas;
import com.planificador.service.DataManager;

/** Pantalla principal: pestañas Reloj e Historial. Sin permiso de archivos muestra cómo darlo y no toca nada. */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.dar_permiso).setOnClickListener(v -> startActivity(
                new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + getPackageName()))));
        BottomNavigationView nav = findViewById(R.id.navegacion);
        nav.setOnItemSelectedListener(item -> { mostrar(); return true; });
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean permiso = Rutas.tienePermiso();
        findViewById(R.id.sin_permiso).setVisibility(permiso ? View.GONE : View.VISIBLE);
        findViewById(R.id.contenido).setVisibility(permiso ? View.VISIBLE : View.GONE);
        findViewById(R.id.navegacion).setVisibility(permiso ? View.VISIBLE : View.GONE);
        if (!permiso) return;
        Datos.asegurar(this);
        DataManager.getInstance().recargar();
        ControlReloj.recuperar(this);
        mostrar();
    }

    /** Texto de prueba hasta que existan las pantallas (Tareas 4 y 5). */
    private void mostrar() {
        DataManager dm = DataManager.getInstance();
        TextView t = new TextView(this);
        t.setPadding(48, 48, 48, 48);
        t.setText(dm.getSubjects().size() + " materias, " + dm.getStudyLogs().size() + " sesiones"
                + (dm.getLoadWarning() != null ? "\n\n" + dm.getLoadWarning() : ""));
        android.widget.FrameLayout c = findViewById(R.id.contenido);
        c.removeAllViews();
        c.addView(t);
    }
}

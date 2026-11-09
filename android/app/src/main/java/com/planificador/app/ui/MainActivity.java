package com.planificador.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

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
        nav.setOnItemSelectedListener(item -> {
            nav.post(this::mostrar);   // el ítem elegido se actualiza después de este oyente
            return true;
        });
        // Android 13+: sin este permiso no suenan ni se ven los avisos de fin de fase.
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
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
        // Solo la primera vez: después cada pestaña se refresca sola en su onResume.
        if (getSupportFragmentManager().findFragmentById(R.id.contenido) == null) mostrar();
    }

    /** Muestra la pestaña elegida. El Historial llega en la Tarea 5. */
    private void mostrar() {
        BottomNavigationView nav = findViewById(R.id.navegacion);
        androidx.fragment.app.Fragment f = nav.getSelectedItemId() == R.id.nav_historial
                ? new HistorialFragment() : new RelojFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.contenido, f).commitAllowingStateLoss();
    }
}

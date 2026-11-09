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

    /** Muestra la pestaña elegida (y descarta las pantallas abiertas desde Más). */
    private void mostrar() {
        BottomNavigationView nav = findViewById(R.id.navegacion);
        int id = nav.getSelectedItemId();
        androidx.fragment.app.Fragment f = id == R.id.nav_reloj ? new RelojFragment()
                : id == R.id.nav_guia ? new GuiaFragment()
                : id == R.id.nav_historial ? new HistorialFragment()
                : id == R.id.nav_mas ? new MasFragment()
                : new HoyFragment();
        androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();
        if (fm.isStateSaved()) return;
        fm.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        fm.beginTransaction().replace(R.id.contenido, f).commit();
    }

    /** Abre una pantalla encima de la actual (desde Más); atrás vuelve. */
    void abrir(androidx.fragment.app.Fragment f) {
        if (getSupportFragmentManager().isStateSaved()) return;
        getSupportFragmentManager().beginTransaction().replace(R.id.contenido, f).addToBackStack(null).commit();
    }

    /**
     * ▶ Cronómetro de una sesión de la guía: deja listo el cronómetro (sin arrancarlo) y va a la pestaña Reloj.
     * Si ya hay un reloj andando o a medias, no lo pisa.
     */
    void cronometroDe(com.planificador.model.StudySession s) {
        boolean[] ocupado = {false};
        ControlReloj.cambiar(this, x -> {
            // Se decide dentro del candado: nunca pisa un reloj andando, a medias, en descanso, con nota o con ciclos.
            if (x != null && (x.corriendo || x.estudiadoSeg > 0 || x.restanteSeg != x.faseSeg || !x.esEstudio()
                    || x.note != null || x.ciclos > 0)) {
                ocupado[0] = true;
                return new com.planificador.app.logica.Reloj.Resultado(x, null, false);
            }
            return new com.planificador.app.logica.Reloj.Resultado(com.planificador.app.logica.Reloj.nuevoCronometro(
                    s.getDurationMinutes(), s.getSubjectId(), s.getTopicDescription(), s.getId()), null, false);
        });
        if (ocupado[0]) {
            Vistas.aviso(this, "Ya hay un reloj en curso. Terminalo o descartalo en la pestaña Reloj.");
            return;
        }
        BottomNavigationView nav = findViewById(R.id.navegacion);
        nav.setSelectedItemId(R.id.nav_reloj);
    }
}

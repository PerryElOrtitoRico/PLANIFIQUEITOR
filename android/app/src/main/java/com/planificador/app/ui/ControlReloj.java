package com.planificador.app.ui;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.planificador.app.R;
import com.planificador.app.datos.AlmacenReloj;
import com.planificador.app.datos.Datos;
import com.planificador.app.logica.EstadoReloj;
import com.planificador.app.logica.Reloj;
import com.planificador.service.DataManager;

import java.io.IOException;
import java.util.function.Function;

/**
 * El único lugar que cambia el estado del reloj y aplica un Resultado: guarda la sesión, después el estado, avisa y
 * programa la alarma y el servicio. Todo bajo el mismo candado, así la pantalla, el servicio y la alarma no se pisan.
 */
public final class ControlReloj {

    static final String CANAL_AVISO = "aviso";
    static final String CANAL_RELOJ = "reloj";
    static final String ACCION_PAUSAR = "com.planificador.app.PAUSAR";
    private static final String TAG = "ControlReloj";
    private static AlmacenReloj almacen;

    private ControlReloj() { }

    static synchronized AlmacenReloj almacen(Context c) {
        if (almacen == null) almacen = new AlmacenReloj(c.getFilesDir().toPath().resolve("reloj.json"));
        return almacen;
    }

    /** Estado actual (null si nunca se usó el reloj o el archivo está dañado). */
    public static synchronized EstadoReloj estado(Context c) {
        try {
            return almacen(c).leer();
        } catch (IOException e) {
            Log.w(TAG, "No se pudo leer el reloj", e);
            return null;
        }
    }

    /**
     * Lee el estado, primero lo recupera (reinicio o fase vencida) y después le aplica f, sin que nadie escriba en el
     * medio. Así pausar o terminar nunca cuentan el tiempo que el teléfono estuvo apagado.
     */
    public static synchronized void cambiar(Context c, Function<EstadoReloj, Reloj.Resultado> f) {
        Reloj.Resultado rec = Reloj.recuperar(estado(c), Datos.ajustes(c),
                System.currentTimeMillis(), SystemClock.elapsedRealtime());
        if (!aplicar(c, rec)) return;   // no se pudo guardar la sesión recuperada: no se toca nada más
        aplicar(c, f.apply(rec.estado()));
    }

    /** Al abrir la app, al sonar la alarma, en cada latido y al prender el teléfono. */
    public static synchronized void recuperar(Context c) {
        cambiar(c, e -> new Reloj.Resultado(e, null, false));
    }

    /** Latido del servicio: solo si el reloj sigue corriendo. */
    static synchronized void latido(Context c) {
        EstadoReloj e = estado(c);
        if (e == null || !e.corriendo) return;
        guardar(c, Reloj.latido(e, System.currentTimeMillis(), SystemClock.elapsedRealtime()));
    }

    /** @return false si había una sesión y no se pudo guardar (sin permiso): el estado queda como estaba. */
    private static boolean aplicar(Context c, Reloj.Resultado r) {
        if (r.sesion() != null) {
            // Primero la sesión y después el estado: un corte en el medio a lo sumo repite la misma sesión (mismo id).
            if (!Datos.asegurar(c)) return false;
            // Si no llegó al disco (carpeta llena o sin acceso), el estado no avanza: el próximo intento la repite.
            if (!DataManager.getInstance().addStudyLog(r.sesion())) return false;
        }
        guardar(c, r.estado());
        if (r.avisar()) avisar(c, r);
        programar(c, r.estado());
        return true;
    }

    private static void guardar(Context c, EstadoReloj e) {
        try {
            if (e == null) almacen(c).borrar(); else almacen(c).guardar(e);
        } catch (IOException ex) {
            Log.w(TAG, "No se pudo guardar el reloj", ex);
        }
    }

    /** Si corre: alarma exacta en finMs y servicio con la notificación. Si no: se cancelan. */
    private static void programar(Context c, EstadoReloj e) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        PendingIntent pi = PendingIntent.getBroadcast(c, 0, new Intent(c, AlarmaReloj.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Intent servicio = new Intent(c, ServicioReloj.class);
        if (e != null && e.corriendo) {
            // USE_EXACT_ALARM la da sola en Android 13+; si igual faltara, una alarma aproximada y el latido del
            // servicio (cada 30 s) terminan la fase.
            if (am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, e.finMs, pi);
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, e.finMs, pi);
            if (!ServicioReloj.activo) {
                try {
                    c.startForegroundService(servicio);
                } catch (IllegalStateException ex) {   // Android no deja arrancarlo desde segundo plano
                    Log.w(TAG, "No se pudo arrancar el servicio del reloj; la alarma igual termina la fase", ex);
                }
            }
        } else {
            am.cancel(pi);
            c.stopService(servicio);
        }
    }

    private static void avisar(Context c, Reloj.Resultado r) {
        crearCanales(c);
        String texto = r.sesion() != null ? "Terminó el estudio: " + r.sesion().getMinutes() + " min guardados"
                : "Terminó el descanso";
        NotificationCompat.Builder n = new NotificationCompat.Builder(c, CANAL_AVISO)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("ESTUDIEITOR")
                .setContentText(texto)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(abrirApp(c));
        c.getSystemService(NotificationManager.class).notify(2, n.build());
    }

    static PendingIntent abrirApp(Context c) {
        return PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE);
    }

    static void crearCanales(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel aviso = new NotificationChannel(CANAL_AVISO, "Fin de fase", NotificationManager.IMPORTANCE_HIGH);
        aviso.enableVibration(true);
        aviso.setVibrationPattern(new long[]{0, 400, 200, 400});
        nm.createNotificationChannel(aviso);
        nm.createNotificationChannel(new NotificationChannel(CANAL_RELOJ, "Reloj en curso", NotificationManager.IMPORTANCE_LOW));
    }
}

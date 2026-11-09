package com.planificador.app.ui;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import com.planificador.app.R;
import com.planificador.app.logica.EstadoReloj;

/**
 * Mientras el reloj corre: notificación con la cuenta regresiva y un latido cada 30 s (para saber hasta cuándo
 * anduvo si el teléfono se apaga). También termina la fase si la alarma no sonó.
 */
public class ServicioReloj extends Service {

    private static final long LATIDO_MS = 30_000;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tarea = this::latir;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ControlReloj.crearCanales(this);
        EstadoReloj e = ControlReloj.estado(this);
        startForeground(1, notificacion(e), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        if (e == null || !e.corriendo) {
            stopSelf();
            return START_NOT_STICKY;
        }
        handler.removeCallbacks(tarea);
        handler.postDelayed(tarea, LATIDO_MS);
        return START_STICKY;
    }

    private void latir() {
        ControlReloj.latido(this);
        ControlReloj.recuperar(this);   // si la fase venció y la alarma no sonó, la termina acá
        EstadoReloj e = ControlReloj.estado(this);
        if (e == null || !e.corriendo) {
            stopSelf();
            return;
        }
        getSystemService(android.app.NotificationManager.class).notify(1, notificacion(e));
        handler.postDelayed(tarea, LATIDO_MS);
    }

    private android.app.Notification notificacion(EstadoReloj e) {
        String titulo = e == null ? "ESTUDIEITOR" : e.esCronometro() ? "Cronómetro"
                : e.esEstudio() ? "Estudio" : "Descanso";
        PendingIntent pausar = PendingIntent.getBroadcast(this, 1,
                new Intent(this, AlarmaReloj.class).setAction(ControlReloj.ACCION_PAUSAR), PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder n = new NotificationCompat.Builder(this, ControlReloj.CANAL_RELOJ)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(titulo)
                .setContentText(e != null && e.topicTitle != null ? e.topicTitle : "Reloj en curso")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(ControlReloj.abrirApp(this))
                .addAction(0, "Pausar", pausar);
        if (e != null && e.corriendo) n.setUsesChronometer(true).setChronometerCountDown(true).setWhen(e.finMs);
        return n.build();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tarea);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

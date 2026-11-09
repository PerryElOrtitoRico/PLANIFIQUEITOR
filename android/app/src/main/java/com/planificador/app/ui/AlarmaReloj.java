package com.planificador.app.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.planificador.app.logica.Reloj;

/** La alarma exacta de fin de fase y el botón Pausar de la notificación. */
public class AlarmaReloj extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        if (ControlReloj.ACCION_PAUSAR.equals(intent.getAction())) {
            ControlReloj.cambiar(c, e -> new Reloj.Resultado(
                    e == null ? null : Reloj.pausar(e, System.currentTimeMillis()), null, false));
        } else {
            ControlReloj.recuperar(c);
        }
    }
}

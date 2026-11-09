package com.planificador.app.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Al prender el teléfono: si el reloj andaba, recuperar() guarda el estudio hasta el último latido.
 * Al apagarse (también por batería baja): un último latido, así el corte se cuenta hasta ese momento y no hasta el
 * último latido de 30 s, que con el teléfono dormido puede ser mucho más viejo.
 */
public class ArranqueReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) ControlReloj.recuperar(c);
        else if (Intent.ACTION_SHUTDOWN.equals(intent.getAction())) ControlReloj.latido(c);
    }
}

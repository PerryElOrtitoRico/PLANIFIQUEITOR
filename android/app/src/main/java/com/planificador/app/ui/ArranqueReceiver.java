package com.planificador.app.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Al prender el teléfono: si el reloj andaba, recuperar() guarda el estudio hasta el último latido. */
public class ArranqueReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) ControlReloj.recuperar(c);
    }
}

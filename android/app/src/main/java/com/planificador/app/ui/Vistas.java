package com.planificador.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;
import java.util.function.Consumer;

/** Piezas de interfaz armadas por código, para no repetir un layout XML por pantalla. */
final class Vistas {

    static final int VERDE = Color.rgb(0x43, 0xA0, 0x47);
    static final int ROJO = Color.rgb(0xE5, 0x39, 0x35);

    private Vistas() { }

    static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static LinearLayout columna(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout fila(Context c, View... vistas) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(android.view.Gravity.CENTER_VERTICAL);
        for (View v : vistas) l.addView(v);
        return l;
    }

    static TextView texto(Context c, String s) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(15);
        t.setPadding(0, dp(c, 4), 0, dp(c, 4));
        return t;
    }

    static TextView titulo(Context c, String s) {
        TextView t = texto(c, s);
        t.setTextSize(20);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(c, 8), 0, dp(c, 8));
        return t;
    }

    static TextView subtitulo(Context c, String s) {
        TextView t = texto(c, s);
        t.setTextSize(17);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(c, 12), 0, dp(c, 4));
        return t;
    }

    static Button boton(Context c, String s, Runnable accion) {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setOnClickListener(v -> accion.run());
        return b;
    }

    static CheckBox casilla(Context c, String s, boolean marcada, Consumer<Boolean> alCambiar) {
        CheckBox cb = new CheckBox(c);
        cb.setText(s);
        cb.setChecked(marcada);
        cb.setOnCheckedChangeListener((v, m) -> alCambiar.accept(m));
        return cb;
    }

    /** Recuadro con borde (verde si `destacado`) alrededor de las vistas. */
    static LinearLayout tarjeta(Context c, boolean destacado, View... vistas) {
        LinearLayout l = columna(c);
        int p = dp(c, 10);
        l.setPadding(p, p, p, p);
        GradientDrawable fondo = new GradientDrawable();
        fondo.setCornerRadius(dp(c, 4));
        fondo.setStroke(dp(c, destacado ? 2 : 1), destacado ? VERDE : Color.GRAY);
        l.setBackground(fondo);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(c, 4), 0, dp(c, 4));
        l.setLayoutParams(lp);
        for (View v : vistas) l.addView(v);
        return l;
    }

    static Spinner selector(Context c, List<String> opciones, int elegido) {
        Spinner s = new Spinner(c);
        s.setAdapter(new ArrayAdapter<>(c, android.R.layout.simple_spinner_dropdown_item, opciones));
        if (elegido >= 0 && elegido < opciones.size()) s.setSelection(elegido);
        return s;
    }

    static EditText campo(Context c, String pista, String valor, boolean numerico) {
        EditText e = new EditText(c);
        e.setHint(pista);
        e.setText(valor);
        e.setInputType(numerico ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        return e;
    }

    static EditText campoMultilinea(Context c, String pista) {
        EditText e = campo(c, pista, "", false);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        e.setMinLines(4);
        return e;
    }

    /** Etiqueta arriba y la vista debajo (para los formularios). */
    static LinearLayout etiquetado(Context c, String etiqueta, View v) {
        LinearLayout l = columna(c);
        l.addView(texto(c, etiqueta));
        l.addView(v);
        return l;
    }

    /**
     * Diálogo con un formulario. `alGuardar` devuelve un texto de error (el diálogo queda abierto para corregir)
     * o null si se guardó.
     */
    static void formulario(Context c, String titulo, View contenido, java.util.function.Supplier<String> alGuardar) {
        android.widget.ScrollView sv = new android.widget.ScrollView(c);
        int p = dp(c, 16);
        contenido.setPadding(p, p / 2, p, 0);
        sv.addView(contenido);
        AlertDialog d = new AlertDialog.Builder(c).setTitle(titulo).setView(sv)
                .setPositiveButton("Guardar", null).setNegativeButton("Cancelar", null).create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String error = alGuardar.get();
            if (error == null) d.dismiss();
            else aviso(c, error);
        }));
        d.show();
    }

    static void confirmar(Context c, String mensaje, Runnable si) {
        new AlertDialog.Builder(c).setMessage(mensaje)
                .setPositiveButton("Sí", (d, w) -> si.run()).setNegativeButton("No", null).show();
    }

    static void aviso(Context c, String mensaje) {
        new AlertDialog.Builder(c).setTitle("Atención").setMessage(mensaje).setPositiveButton("OK", null).show();
    }

    /** Entero del campo, o `porDefecto` si no es un número (como la PC). */
    static int entero(EditText e, int porDefecto) {
        try {
            return Integer.parseInt(e.getText().toString().trim());
        } catch (NumberFormatException ex) {
            return porDefecto;
        }
    }
}

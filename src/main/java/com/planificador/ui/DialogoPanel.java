package com.planificador.ui;

import com.planificador.panel.PanelSincronizador;
import com.planificador.panel.PanelSincronizador.Instantanea;
import com.planificador.panel.PanelSincronizador.Resumen;
import panel.cliente.ConexionPanel;
import panel.cliente.NombreDispositivo;
import panel.cliente.PanelCompartido;
import panel.cliente.ValidacionConexion;
import panel.core.ConfigPanel;
import panel.core.PanelClient;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * Ventana "Conexión con el panel": dirección, token, dispositivo, pruebas y reenvío del historial.
 * Nada de red ni de archivos corre en el hilo de eventos salvo leer el archivo de configuración al abrir.
 * El token nunca se muestra en un mensaje.
 */
final class DialogoPanel extends JDialog {

    private final PanelCompartido panel;
    private final PanelSincronizador sincronizador;
    private final Path carpetaDatos;

    private final JTextField url = new JTextField(32);
    private final JPasswordField token = new JPasswordField(32);
    private final JCheckBox mostrar = new JCheckBox("Mostrar");
    private final JTextField dispositivo = new JTextField(32);
    private final JCheckBox enviar = new JCheckBox("Enviar al panel");
    private final JButton probar = new JButton("Probar conexión");
    private final JButton reenviar = new JButton("Reenviar todo al panel");
    private final JButton guardar = new JButton("Guardar");
    private final JButton cancelar = new JButton("Cancelar");
    private final JButton confirmar = new JButton("Confirmar borrados");
    private final JLabel retenidosLinea = new JLabel(" ");
    private final JLabel estado = new JLabel(" ");
    private boolean ocupado;

    DialogoPanel(Frame dueno, PanelCompartido panel, PanelSincronizador sincronizador, Path carpetaDatos) {
        super(dueno, "Conexión con el panel", true);
        this.panel = panel;
        this.sincronizador = sincronizador;
        this.carpetaDatos = carpetaDatos;
        prellenar();

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));
        int fila = 0;
        formulario.add(new JLabel("Dirección del panel:"), celda(0, fila));
        GridBagConstraints campo = celda(1, fila++);
        campo.gridwidth = 2;
        formulario.add(url, campo);
        formulario.add(new JLabel("Token:"), celda(0, fila));
        formulario.add(token, celda(1, fila));
        formulario.add(mostrar, celda(2, fila++));
        formulario.add(new JLabel("Dispositivo:"), celda(0, fila));
        campo = celda(1, fila++);
        campo.gridwidth = 2;
        formulario.add(dispositivo, campo);
        campo = celda(1, fila++);
        campo.gridwidth = 2;
        formulario.add(enviar, campo);
        campo = celda(0, fila);
        campo.gridwidth = 3;
        formulario.add(estado, campo);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(confirmar);
        botones.add(probar);
        botones.add(reenviar);
        botones.add(Box.createHorizontalStrut(12));
        botones.add(guardar);
        botones.add(cancelar);

        setLayout(new BorderLayout());
        JPanel retenidosPanel = new JPanel(new BorderLayout());
        retenidosPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 12));
        retenidosPanel.add(retenidosLinea, BorderLayout.CENTER);
        JPanel abajo = new JPanel(new BorderLayout());
        abajo.add(retenidosPanel, BorderLayout.NORTH);
        abajo.add(botones, BorderLayout.SOUTH);
        add(formulario, BorderLayout.CENTER);
        add(abajo, BorderLayout.SOUTH);

        char ocultar = token.getEchoChar();
        mostrar.addActionListener(e -> token.setEchoChar(mostrar.isSelected() ? (char) 0 : ocultar));
        probar.addActionListener(e -> probarConexion());
        reenviar.addActionListener(e -> reenviarTodo());
        confirmar.addActionListener(e -> confirmarBorrados());
        guardar.addActionListener(e -> guardarConfiguracion());
        cancelar.addActionListener(e -> dispose());

        // Mientras corre un worker la ventana no se cierra: así done() siempre encuentra su ventana.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!DialogoPanel.this.ocupado) dispose();
            }
        });

        reenviar.setToolTipText("Usa la conexión guardada, no lo escrito acá sin guardar.");
        refrescarBotones();
        if (!guardadaActiva()) estado.setText(TextosPlanificador.notaInactiva());
        getRootPane().setDefaultButton(guardar);
        pack();
        setLocationRelativeTo(dueno);
    }

    private static GridBagConstraints celda(int x, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.anchor = GridBagConstraints.WEST;
        c.fill = x == 1 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
        c.weightx = x == 1 ? 1 : 0;
        c.insets = new Insets(3, 3, 3, 3);
        return c;
    }

    /** Llena los campos con lo guardado. */
    private void prellenar() {
        ConfigPanel cfg = ConfigPanel.cargar(carpetaDatos.resolve("panel-cliente.properties"));
        url.setText(cfg.url());
        token.setText(cfg.token());
        dispositivo.setText(cfg.dispositivo().isEmpty() ? nombreDeEstaPc() : cfg.dispositivo());
        boolean hayDatos = !cfg.url().isEmpty() && !cfg.token().isEmpty();
        enviar.setSelected(!hayDatos || cfg.activo());
    }

    private static String nombreDeEstaPc() {
        return NombreDispositivo.porDefecto(System.getenv());
    }

    private String urlEscrita() {
        return url.getText().trim();
    }

    private String tokenEscrito() {
        return new String(token.getPassword()).trim();
    }

    private String dispositivoEscrito() {
        String d = dispositivo.getText().trim();
        return d.isEmpty() ? nombreDeEstaPc() : d;
    }

    private boolean guardadaActiva() {
        ConexionPanel guardada = panel.get();
        return guardada != null && guardada.activo();
    }

    /** Muestra los errores de validación; devuelve true si no había ninguno. */
    private boolean validar() {
        List<String> errores = ValidacionConexion.errores(urlEscrita(), tokenEscrito());
        if (errores.isEmpty()) return true;
        JOptionPane.showMessageDialog(this, String.join("\n", errores), "Revisá los datos", JOptionPane.WARNING_MESSAGE);
        return false;
    }

    private void refrescarBotones() {
        probar.setEnabled(!ocupado);
        guardar.setEnabled(!ocupado);
        cancelar.setEnabled(!ocupado);
        reenviar.setEnabled(!ocupado && guardadaActiva());
        int retenidos = sincronizador.borradosRetenidos();
        boolean hayRetenidos = retenidos > 0 && guardadaActiva();
        confirmar.setVisible(hayRetenidos);
        retenidosLinea.setText(hayRetenidos ? TextosPlanificador.borradosRetenidos(retenidos) : " ");
        confirmar.setEnabled(!ocupado && hayRetenidos);
    }

    private void ocupado(boolean si, String texto) {
        ocupado = si;
        refrescarBotones();
        estado.setText(texto);
        setCursor(si ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }

    private void probarConexion() {
        if (!validar()) return;
        String u = urlEscrita();
        String t = tokenEscrito();
        ocupado(true, "Probando…");
        new SwingWorker<PanelClient.Conexion, Void>() {
            @Override
            protected PanelClient.Conexion doInBackground() {
                return ConexionPanel.probarValores(u, t);
            }

            @Override
            protected void done() {
                ocupado(false, " ");
                PanelClient.Conexion res;
                try {
                    res = get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    res = PanelClient.Conexion.SIN_RESPUESTA;
                } catch (ExecutionException | CancellationException e) {
                    res = PanelClient.Conexion.SIN_RESPUESTA;
                }
                JOptionPane.showMessageDialog(DialogoPanel.this, TextosPlanificador.prueba(res), "Probar conexión",
                        res == PanelClient.Conexion.OK ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
            }
        }.execute();
    }

    private void reenviarTodo() {
        ConexionPanel conexion = panel.get();
        if (conexion == null || !conexion.activo()) return;
        // Los datos se copian acá, en el hilo de eventos, que es el que los modifica.
        Instantanea foto = sincronizador.tomar();
        ocupado(true, "Reenviando…");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                Resumen resumen = sincronizador.reenviarTodo(foto);
                conexion.vaciarAhora();
                return TextosPlanificador.reenvio(resumen, conexion.pendientes());
            }

            @Override
            protected void done() {
                ocupado(false, " ");
                try {
                    JOptionPane.showMessageDialog(DialogoPanel.this, get(), "Reenviar todo al panel",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException | CancellationException e) {
                    JOptionPane.showMessageDialog(DialogoPanel.this,
                            TextosPlanificador.errorAlReenviar(e.getCause() != null ? e.getCause() : e), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void confirmarBorrados() {
        ConexionPanel conexion = panel.get();
        if (conexion == null || !conexion.activo()) return;
        // Los datos se copian acá, en el hilo de eventos, que es el que los modifica.
        Instantanea foto = sincronizador.tomar();
        ocupado(true, "Borrando…");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                Resumen resumen = sincronizador.confirmarBorrados(foto);
                conexion.vaciarAhora();
                return TextosPlanificador.borradosConfirmados(resumen, conexion.pendientes());
            }

            @Override
            protected void done() {
                ocupado(false, " ");
                try {
                    JOptionPane.showMessageDialog(DialogoPanel.this, get(), "Confirmar borrados",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException | CancellationException e) {
                    JOptionPane.showMessageDialog(DialogoPanel.this,
                            TextosPlanificador.errorAlReenviar(e.getCause() != null ? e.getCause() : e), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void guardarConfiguracion() {
        if (!validar()) return;
        String u = urlEscrita();
        String t = tokenEscrito();
        String d = dispositivoEscrito();
        boolean enviarAlPanel = enviar.isSelected();
        ocupado(true, "Guardando…");
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                // Sin ninguna corrida en vuelo: cierra la conexión vieja y recién después abre la nueva.
                // Si guardar la configuración falla, no se cambia nada.
                try {
                    sincronizador.conConexionDetenida(() -> {
                        try {
                            ConexionPanel.guardarConfig(carpetaDatos, u, t, d, enviarAlPanel);
                        } catch (java.io.IOException e) {
                            throw new java.io.UncheckedIOException(e);
                        }
                        panel.reabrir(() -> ConexionPanel.abrir(carpetaDatos));
                    });
                } catch (java.io.UncheckedIOException e) {
                    throw e.getCause();
                }
                ConexionPanel nueva = panel.get();
                boolean activa = nueva != null && nueva.activo();
                // Si cambió el servidor, el archivo de estado lo sabe (recuerda su dirección) y lo reenvía solo.
                return activa;
            }

            @Override
            protected void done() {
                boolean activa;
                try {
                    activa = get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    ocupado(false, " ");
                    return;
                } catch (ExecutionException | CancellationException e) {
                    ocupado(false, " ");
                    JOptionPane.showMessageDialog(DialogoPanel.this,
                            TextosPlanificador.errorAlGuardar(e.getCause() != null ? e.getCause() : e), "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!TextosPlanificador.debeCerrarTrasGuardar(enviarAlPanel, activa)) {
                    ocupado(false, TextosPlanificador.notaInactiva());
                    JOptionPane.showMessageDialog(DialogoPanel.this, TextosPlanificador.notaInactiva(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                ocupado(false, " ");
                sincronizador.sincronizarAsync(); // manda el historial la primera vez o al cambiar de servidor
                dispose();
            }
        }.execute();
    }
}

package com.planificador;

import com.planificador.panel.DatosDeDataManager;
import com.planificador.panel.EstadoPanel;
import com.planificador.panel.PanelSincronizador;
import com.planificador.service.DataManager;
import com.planificador.ui.MainWindow;
import panel.cliente.ConexionPanel;
import panel.cliente.PanelCompartido;

import javax.swing.SwingUtilities;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.time.Clock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        com.planificador.ui.Tema.instalar();

        // Ejecutar la ventana principal en el Hilo de Eventos de AWT (EDT)
        SwingUtilities.invokeLater(() -> {
            MainWindow frame = new MainWindow();
            frame.setLocationRelativeTo(null);
            PanelSincronizador sincronizador = conectarPanel(frame);
            frame.setVisible(true);
            // Atrapa lo que cambió mientras el panel estaba apagado o la app cerrada.
            if (sincronizador != null) sincronizador.sincronizarAsync();
        });
    }

    /**
     * Abre la conexión con el panel y la engancha al guardado y al cierre de la ventana. Con el panel apagado no
     * crea archivos, hilos ni tráfico. Un fallo acá nunca impide abrir el Planificador.
     */
    private static PanelSincronizador conectarPanel(MainWindow frame) {
        try {
            DataManager dm = DataManager.getInstance();
            Path archivoDatos = dm.getDataFile();
            Path carpeta = archivoDatos.getParent();
            PanelCompartido holder = new PanelCompartido(ConexionPanel.abrir(carpeta));
            EstadoPanel estado = EstadoPanel.junto(archivoDatos);
            PanelSincronizador sincronizador = new PanelSincronizador(holder, estado, new DatosDeDataManager(),
                    Clock.systemDefaultZone());
            dm.setAlGuardar(sincronizador::sincronizarAsync);
            frame.setPanel(holder, sincronizador, carpeta);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    // Primero la conexión, aislada; después el hilo del sincronizador; luego sale el EXIT_ON_CLOSE.
                    try {
                        holder.close();
                    } catch (RuntimeException ex) {
                        LOG.log(Level.WARNING, "No se pudo cerrar la conexión con el panel: {0}",
                                ex.getClass().getSimpleName());
                    }
                    try {
                        sincronizador.close();
                    } catch (RuntimeException ex) {
                        LOG.log(Level.WARNING, "No se pudo cerrar el sincronizador: {0}",
                                ex.getClass().getSimpleName());
                    }
                }
            });
            return sincronizador;
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "El panel no se pudo preparar: {0}", e.getClass().getSimpleName());
            return null;
        }
    }
}

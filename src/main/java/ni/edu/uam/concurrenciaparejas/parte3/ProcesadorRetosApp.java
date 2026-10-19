package ni.edu.uam.concurrenciaparejas.parte3;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Retos 1, 2 y 3 sobre la aplicación de la Parte 2:
 * <ul>
 *   <li>Reto 1: botón Reiniciar, habilitado solo cuando ambas tareas terminaron.</li>
 *   <li>Reto 2: registro visual en un ListView, alimentado desde el hilo gráfico.</li>
 *   <li>Reto 3: botón para cancelar únicamente al operador A.</li>
 * </ul>
 */
public class ProcesadorRetosApp extends Application {
    private static final int SOLICITUDES = 20;

    private ContadorSeguro contador = new ContadorSeguro();
    private Task<Void> tarea1;
    private Task<Void> tarea2;
    private final Label total = new Label("Procesadas: 0");
    private final Label estado = new Label("Sin iniciar");
    private final ProgressBar barra1 = new ProgressBar(0);
    private final ProgressBar barra2 = new ProgressBar(0);
    private final ListView<String> registro = new ListView<>();
    private final Button iniciar = new Button("Iniciar");
    private final Button cancelarA = new Button("Cancelar A");
    private final Button cancelarTodo = new Button("Cancelar todo");
    private final Button reiniciar = new Button("Reiniciar");

    private static class ContadorSeguro {
        private int valor;
        public synchronized int incrementar() { return ++valor; }
    }

    private Task<Void> crearTarea(String operador, ContadorSeguro contadorDeLaEjecucion) {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (int i = 1; i <= SOLICITUDES; i++) {
                    if (isCancelled()) break;
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException ex) {
                        if (isCancelled()) break;
                        Thread.currentThread().interrupt();
                        throw ex;
                    }
                    if (isCancelled()) break;
                    int actual = contadorDeLaEjecucion.incrementar();
                    updateProgress(i, SOLICITUDES);
                    final int solicitud = i;
                    // Los controles solo se tocan en el hilo gráfico.
                    Platform.runLater(() -> {
                        total.setText("Procesadas: " + actual);
                        registro.getItems().add(operador + " - solicitud " + solicitud);
                        registro.scrollTo(registro.getItems().size() - 1);
                    });
                }
                return null;
            }
        };
    }

    private void actualizarEstado() {
        boolean aTermino = tarea1 != null && tarea1.isDone();
        boolean bTermino = tarea2 != null && tarea2.isDone();
        cancelarA.setDisable(aTermino);
        if (aTermino && bTermino) {
            cancelarTodo.setDisable(true);
            reiniciar.setDisable(false);
            estado.setText("Ambas tareas finalizaron o se cancelaron");
        } else if (aTermino) {
            estado.setText("Operador A terminó o fue cancelado; Operador B sigue procesando");
        }
    }

    private void observar(Task<Void> tarea) {
        tarea.setOnSucceeded(ev -> actualizarEstado());
        tarea.setOnCancelled(ev -> actualizarEstado());
        tarea.setOnFailed(ev -> actualizarEstado());
    }

    private void iniciarEjecucion() {
        iniciar.setDisable(true);
        reiniciar.setDisable(true);
        cancelarA.setDisable(false);
        cancelarTodo.setDisable(false);
        tarea1 = crearTarea("Operador A", contador);
        tarea2 = crearTarea("Operador B", contador);
        barra1.progressProperty().bind(tarea1.progressProperty());
        barra2.progressProperty().bind(tarea2.progressProperty());
        estado.setText("Procesando...");
        observar(tarea1);
        observar(tarea2);
        Thread hilo1 = new Thread(tarea1, "operador-a");
        Thread hilo2 = new Thread(tarea2, "operador-b");
        hilo1.setDaemon(true);
        hilo2.setDaemon(true);
        hilo1.start();
        hilo2.start();
    }

    private void reiniciarEjecucion() {
        // Solo se llega aquí con el botón habilitado, es decir, con ambas tareas terminadas.
        if (tarea1 != null && !tarea1.isDone()) return;
        if (tarea2 != null && !tarea2.isDone()) return;
        contador = new ContadorSeguro();
        barra1.progressProperty().unbind();
        barra2.progressProperty().unbind();
        barra1.setProgress(0);
        barra2.setProgress(0);
        registro.getItems().clear();
        total.setText("Procesadas: 0");
        estado.setText("Listo para una nueva ejecución");
        reiniciar.setDisable(true);
        iniciar.setDisable(false);
    }

    private void cancelarTodo() {
        if (tarea1 != null) tarea1.cancel(true);
        if (tarea2 != null) tarea2.cancel(true);
        cancelarA.setDisable(true);
        cancelarTodo.setDisable(true);
        estado.setText("Cancelación solicitada...");
    }

    @Override
    public void start(Stage ventana) {
        cancelarA.setDisable(true);
        cancelarTodo.setDisable(true);
        reiniciar.setDisable(true);
        registro.setPrefHeight(160);

        iniciar.setOnAction(e -> iniciarEjecucion());
        cancelarA.setOnAction(e -> {
            if (tarea1 != null) tarea1.cancel(true);
            cancelarA.setDisable(true);
            estado.setText("Cancelación del Operador A solicitada...");
        });
        cancelarTodo.setOnAction(e -> cancelarTodo());
        reiniciar.setOnAction(e -> reiniciarEjecucion());

        VBox raiz = new VBox(12, total, estado,
                new Label("Operador A"), barra1,
                new Label("Operador B"), barra2,
                new HBox(8, iniciar, cancelarA, cancelarTodo, reiniciar),
                new Label("Registro"), registro);
        raiz.setStyle("-fx-padding: 20;");
        ventana.setScene(new Scene(raiz, 480, 520));
        ventana.setTitle("Procesamiento concurrente - Retos 1, 2 y 3");
        ventana.setOnCloseRequest(e -> cancelarTodo());
        ventana.show();
    }

    public static void main(String[] args) { launch(args); }
}

package ni.edu.uam.concurrenciaparejas.parte3;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Reto 4: dos operadores retiran unidades de un inventario compartido de 25 sin llegar a negativo. */
public class InventarioApp extends Application {
    private static final int INVENTARIO_INICIAL = 25;

    private Task<Integer> tarea1;
    private Task<Integer> tarea2;
    private final Label existenciasLabel = new Label("Existencias: " + INVENTARIO_INICIAL);
    private final Label estado = new Label("Sin iniciar");
    private final ProgressBar barraInventario = new ProgressBar(1);
    private final ListView<String> registro = new ListView<>();
    private final Button iniciar = new Button("Iniciar");
    private final Button cancelar = new Button("Cancelar");

    private Task<Integer> crearTarea(String operador, Inventario inventario) {
        return new Task<>() {
            @Override
            protected Integer call() throws Exception {
                int retiradas = 0;
                while (!isCancelled()) {
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException ex) {
                        if (isCancelled()) break;
                        Thread.currentThread().interrupt();
                        throw ex;
                    }
                    if (isCancelled()) break;
                    int quedan = inventario.retirarUnidad();
                    if (quedan < 0) break; // inventario agotado
                    retiradas++;
                    final int numero = retiradas;
                    Platform.runLater(() -> {
                        existenciasLabel.setText("Existencias: " + quedan);
                        barraInventario.setProgress((double) quedan / INVENTARIO_INICIAL);
                        registro.getItems().add(operador + " retiró la unidad " + numero
                                + " (quedan " + quedan + ")");
                        registro.scrollTo(registro.getItems().size() - 1);
                    });
                }
                return retiradas;
            }
        };
    }

    private void actualizarEstado() {
        if (tarea1.isDone() && tarea2.isDone()) {
            cancelar.setDisable(true);
            iniciar.setDisable(false);
            String resumen = "";
            if (tarea1.isCancelled() || tarea2.isCancelled()) {
                resumen = "Cancelado. ";
            } else {
                resumen = "Inventario agotado. A retiró " + tarea1.getValue()
                        + " y B retiró " + tarea2.getValue() + ". ";
            }
            estado.setText(resumen + "Puede iniciar otra ejecución.");
        }
    }

    private void observar(Task<Integer> tarea) {
        tarea.setOnSucceeded(ev -> actualizarEstado());
        tarea.setOnCancelled(ev -> actualizarEstado());
        tarea.setOnFailed(ev -> actualizarEstado());
    }

    @Override
    public void start(Stage ventana) {
        cancelar.setDisable(true);
        registro.setPrefHeight(220);

        iniciar.setOnAction(e -> {
            iniciar.setDisable(true);
            cancelar.setDisable(false);
            registro.getItems().clear();
            existenciasLabel.setText("Existencias: " + INVENTARIO_INICIAL);
            barraInventario.setProgress(1);
            estado.setText("Procesando...");
            Inventario inventario = new Inventario(INVENTARIO_INICIAL);
            tarea1 = crearTarea("Operador A", inventario);
            tarea2 = crearTarea("Operador B", inventario);
            observar(tarea1);
            observar(tarea2);
            Thread hilo1 = new Thread(tarea1, "operador-a");
            Thread hilo2 = new Thread(tarea2, "operador-b");
            hilo1.setDaemon(true);
            hilo2.setDaemon(true);
            hilo1.start();
            hilo2.start();
        });
        cancelar.setOnAction(e -> {
            if (tarea1 != null) tarea1.cancel(true);
            if (tarea2 != null) tarea2.cancel(true);
            cancelar.setDisable(true);
            estado.setText("Cancelación solicitada...");
        });

        VBox raiz = new VBox(12, existenciasLabel, estado, barraInventario,
                new HBox(8, iniciar, cancelar), new Label("Registro"), registro);
        raiz.setStyle("-fx-padding: 20;");
        ventana.setScene(new Scene(raiz, 460, 460));
        ventana.setTitle("Inventario compartido - Reto 4");
        ventana.setOnCloseRequest(e -> {
            if (tarea1 != null) tarea1.cancel(true);
            if (tarea2 != null) tarea2.cancel(true);
        });
        ventana.show();
    }

    public static void main(String[] args) { launch(args); }
}

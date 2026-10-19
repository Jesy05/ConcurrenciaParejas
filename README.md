# Práctica guiada · Sincronización y control de hilos en JavaFX

**Materia:** Programación de Aplicaciones de Escritorio · Sesiones 28 y 29

**Equipo**
- Jesy González
- José Cristo Carvallo

---

## Estructura del proyecto

| Parte | Paquete | Contenido |
|-------|---------|-----------|
| 1 | `parte1` | `Carrera` (contador sin proteger), `ContadorSeguro` (`synchronized`) y `CarreraSegura` |
| 2 | `parte2` | `ProcesadorApp`: dos tareas con `Task`, barras de progreso, contador sincronizado y cancelación |
| 3 | `parte3` | `ProcesadorRetosApp` (retos 1, 2 y 3), `Inventario` e `InventarioApp` (reto 4) |

## Cómo ejecutar

Desde IntelliJ IDEA (JDK 21, JavaFX 21), ejecutar el `main` de la clase deseada:

- Parte 1: `parte1.Carrera` y `parte1.CarreraSegura` (consola).
- Parte 2: `parte2.ProcesadorApp`.
- Parte 3: `parte3.ProcesadorRetosApp` (retos 1 a 3) y `parte3.InventarioApp` (reto 4).

## Parte 1 · Resultados del experimento

Sin sincronizar, `Carrera` imprime totales distintos en cada ejecución y siempre inferiores a 200 000
(por ejemplo 107 477, 122 338 y 158 984). Con una instancia compartida de `ContadorSeguro`, `CarreraSegura`
imprime 200 000 en todas las ejecuciones.

## Parte 2 · Limitación de `Platform.runLater()` y mejora propuesta

El contador es seguro, pero cada tarea hace `incrementar()` en su hilo y después encola la actualización
visual con `Platform.runLater()`. Entre ambos pasos el planificador puede intercambiar los hilos, así que la
etiqueta puede recibir primero el valor 12 y después el 11, y mostrar por un instante un total menor al real.
También se pueden encolar muchas actualizaciones si las tareas son rápidas.

**Mejora:** guardar el total en una propiedad observable (por ejemplo un `AtomicInteger` o un
`ReadOnlyIntegerWrapper`) y que la interfaz lea siempre el valor actual, o enlazar la etiqueta a una
propiedad en lugar de enviar el texto desde los hilos. Así lo que se dibuja es el estado más reciente y no
una foto antigua. Otra opción es que las tareas publiquen con `updateMessage()` y que la etiqueta se enlace a
`messageProperty()`, que ya coalesce las actualizaciones.

## Parte 3 · Retos

- **Reto 1, Reiniciar:** el botón solo se habilita cuando `tarea1` y `tarea2` están terminadas (`isDone()`).
  Al pulsarlo se crea un contador nuevo, se desenlazan y reinician las barras, se limpia el registro y se
  vuelve a habilitar Iniciar. Cada ejecución recibe su propio contador, así las tareas de una ejecución
  anterior no pueden tocar la siguiente.
- **Reto 2, `ListView`:** cada tarea agrega "operador - solicitud n" dentro de `Platform.runLater()`, por lo
  que el control solo lo modifica el hilo gráfico.
- **Reto 3, cancelar solo al operador A:** el botón llama a `tarea1.cancel(true)` y deja correr a la
  `tarea2`. El estado indica cuándo A terminó y B sigue, y Reiniciar se habilita solo cuando ambas terminan.
- **Reto 4, inventario de 25 unidades:** `Inventario.retirarUnidad()` verifica y resta dentro del mismo
  método `synchronized`, así que nunca quedan existencias negativas. Cada operador retira hasta agotar el
  inventario; entre ambos suman siempre 25. Se probó con 200 repeticiones de cuatro hilos retirando a la vez:
  en todas se retiraron exactamente 25 unidades y quedó en 0.

## Parte 4 · Análisis y cierre

**1. ¿Qué diferencia existe entre `Thread.sleep()` y `Task.cancel(true)`?**
`Thread.sleep()` pausa el hilo actual durante un tiempo; no cancela nada. `Task.cancel(true)` marca la tarea
como cancelada (`isCancelled()` pasa a `true`) e interrumpe el hilo que la ejecuta. Si ese hilo está dormido
en `sleep()`, la interrupción lo despierta con una `InterruptedException`, y por eso el código captura esa
excepción y revisa `isCancelled()` para salir del ciclo.

**2. ¿Por qué no se debe ejecutar el procesamiento directamente dentro de `setOnAction`?**
Porque `setOnAction` se ejecuta en el hilo de la aplicación JavaFX, el mismo que dibuja la ventana y atiende
los eventos. Si ahí se hace un trabajo largo, la interfaz se congela hasta que termine: no se repinta, no
responde a clics ni deja cancelar. El trabajo va en un `Task` en otro hilo.

**3. ¿Qué protege `synchronized` y qué problema no resuelve por sí solo?**
Protege el acceso a un recurso compartido: solo un hilo a la vez puede ejecutar el bloque o método sincronizado
sobre el mismo objeto, lo que vuelve atómicos `contador++` o "verificar y retirar". No resuelve el orden en que
se ven los resultados (como en el límite de `Platform.runLater()`), ni los interbloqueos si se toman varios
bloqueos en distinto orden, ni la cancelación o la coordinación entre hilos. Tampoco protege el estado que
queda fuera del bloque.

**4. ¿Cuándo utilizaría `Platform.runLater()` y cuándo `updateProgress()`?**
`updateProgress()` (y `updateMessage()`) sirve para informar el avance de la propia tarea: se puede llamar
desde el hilo de trabajo y la actualización de `progressProperty()` ya llega segura al hilo gráfico, por lo
que se enlaza con `bind`. `Platform.runLater()` se usa para cualquier otro cambio de la interfaz desde un
hilo de trabajo, como cambiar el texto de una etiqueta o agregar un elemento a un `ListView`.

**5. ¿Por qué la cancelación de un hilo es cooperativa?**
Porque Java no puede detener a la fuerza un hilo de forma segura: matarlo en cualquier punto podría dejar un
recurso a medias o un bloqueo sin liberar. `cancel(true)` solo pide la detención (bandera de cancelación e
interrupción); es el código de la tarea el que revisa `isCancelled()` o atiende la interrupción y termina de
forma ordenada, liberando lo que haya usado.

## Evidencias

Las capturas de ejecución completa y de cancelación se entregan junto con este documento.

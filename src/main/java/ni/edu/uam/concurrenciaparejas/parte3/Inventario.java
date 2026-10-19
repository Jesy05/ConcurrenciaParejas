package ni.edu.uam.concurrenciaparejas.parte3;

/** Recurso compartido del Reto 4: existencias que nunca pueden quedar en negativo. */
public class Inventario {
    private int existencias;

    public Inventario(int existenciasIniciales) {
        this.existencias = existenciasIniciales;
    }

    /**
     * Retira una unidad. La verificación y la resta ocurren dentro del mismo bloque sincronizado,
     * de modo que dos operadores no pueden pasar la verificación con la última unidad a la vez.
     *
     * @return las existencias que quedan tras el retiro, o -1 si no había unidades
     */
    public synchronized int retirarUnidad() {
        if (existencias <= 0) {
            return -1;
        }
        existencias--;
        return existencias;
    }

    public synchronized int obtener() {
        return existencias;
    }
}

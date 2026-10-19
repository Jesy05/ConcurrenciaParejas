package ni.edu.uam.concurrenciaparejas.parte1;


public class ContadorSeguro {
    private int valor = 0;

    public synchronized void incrementar() {
        valor++;
    }

    public synchronized int obtener() {
        return valor;
    }
}

package ni.edu.uam.concurrenciaparejas.parte1;


public class Carrera {
    static int contador = 0;

    public static void main(String[] args) throws InterruptedException {
        Runnable trabajo = () -> {
            for (int i = 0; i < 100_000; i++) {
                contador++;
            }
        };

        Thread a = new Thread(trabajo);
        Thread b = new Thread(trabajo);
        a.start();
        b.start();
        a.join();
        b.join();
        System.out.println("Total: " + contador);
    }
}

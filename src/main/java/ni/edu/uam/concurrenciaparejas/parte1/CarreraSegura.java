package ni.edu.uam.concurrenciaparejas.parte1;

public class CarreraSegura {

    public static void main(String[] args) throws InterruptedException {
        ContadorSeguro contador = new ContadorSeguro();

        Runnable trabajo = () -> {
            for (int i = 0; i < 100_000; i++) {
                contador.incrementar();
            }
        };

        Thread a = new Thread(trabajo);
        Thread b = new Thread(trabajo);
        a.start();
        b.start();
        a.join();
        b.join();
        System.out.println("Total: " + contador.obtener());
    }
}

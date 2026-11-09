import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) {

        ExecutorService executor = Executors.newFixedThreadPool(3);

        Runnable task1 = () -> {
            System.out.println("Tâche 1 exécutée par : "
                    + Thread.currentThread().getName());
        };

        Runnable task2 = () -> {
            System.out.println("Tâche 2 exécutée par : "
                    + Thread.currentThread().getName());
        };

        Runnable task3 = () -> {
            System.out.println("Tâche 3 exécutée par : "
                    + Thread.currentThread().getName());
        };

        executor.submit(task1);
        executor.submit(task2);
        executor.submit(task3);

        executor.shutdown();
    }
}
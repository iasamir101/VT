package fr.vt.simulation.threadclassique.executor.etape2_callablefuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Démo Callable et Future : récupérer le résultat d'une tâche exécutée dans un pool.
 *
 * But : montrer la différence avec {@link Runnable} : un {@link Callable} renvoie une valeur, que l'on récupère via le {@link Future} renvoyé par {@code submit()}.
 *
 * Entrée : aucune. Une tâche qui attend 2 secondes puis renvoie 10 + 20.
 *
 * Sortie : le résultat du calcul.
 *
 * Exemple de sortie attendue (l'ordre peut varier d'une exécution à l'autre) :
 * <pre>
 * Résultat : 30
 * </pre>
 *
 * À observer : le résultat s'affiche après environ 2 secondes : {@code future.get()} bloque le thread principal tant que la tâche n'est pas finie.
 *
 * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.executor.etape2_callablefuture.CallableFutureDemo}
 */
public class CallableFutureDemo {

    public static void main(String[] args) throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(3);

        try {
            // Contrairement à Runnable, Callable renvoie une valeur et peut lever une exception
            Callable<Integer> task1 = () -> {
                //Thread.sleep(2000); // calcul long simulé
                return 10;
            };
            Callable<Integer> task2 = () -> {
                Thread.sleep(2000); // calcul long simulé
                return 20;
            };
            Callable<Integer> task3 = () -> {
                Thread.sleep(2000); // calcul long simulé
                return 30;
            };


            // submit() rend la main tout de suite ; la tâche tourne dans un thread du pool
            Future<Integer> future1 = executor.submit(task1);
            Future<Integer> future2 = executor.submit(task2);
            Future<Integer> future3 = executor.submit(task3);

            // get() bloque le thread principal jusqu'à ce que le résultat soit prêt (~2 s)
            System.out.println("Résultat : " + future1.get());
            System.out.println("Résultat : " + future2.get());
            System.out.println("Résultat : " + future3.get()) ;
            System.out.println("terminé  ");

            // ---------------------------------------------------------------
            // invokeAll : soumettre toute une collection de tâches en un seul appel.
            // Contrairement à submit(), invokeAll() BLOQUE jusqu'à ce que TOUTES les tâches soient finies :
            // les futures renvoyés sont donc déjà terminés et get() ne bloque plus.
            // ---------------------------------------------------------------
            long start = System.currentTimeMillis();
            List<Future<Integer>> futures = executor.invokeAll(collection());
            long elapsed = System.currentTimeMillis() - start;

            // Tâches de 1 s, 2 s et 3 s lancées en parallèle : on attend la plus longue (~3 s), pas la somme (6 s)
            System.out.println("invokeAll terminé après ~" + elapsed + " ms");
            for (Future<Integer> future : futures) {
                // isDone() vaut true pour tous : invokeAll a déjà attendu
                System.out.println("Résultat : " + future.get() + " (terminé : " + future.isDone() + ")");
            }

        } finally {
            executor.shutdown();
        }
    }

    /**
     * Fabrique les tâches passées à invokeAll : trois Callable de durées différentes (1 s, 2 s, 3 s)
     * qui renvoient 1, 2 et 3.
     */
    private static List<Callable<Integer>> collection() {
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            int value = i;
            tasks.add(() -> {
                Thread.sleep(value * 1000L); // calcul long simulé : 1 s, 2 s, 3 s
                return value;
            });
        }
        return tasks;
    }
}

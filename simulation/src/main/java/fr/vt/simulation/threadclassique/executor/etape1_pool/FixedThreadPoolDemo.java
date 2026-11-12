package fr.vt.simulation.threadclassique.executor.etape1_pool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Démo ExecutorService : trois tâches soumises à un pool fixe de 3 threads.
 *
 * But : montrer qu'un pool réutilise des threads nommés {@code pool-N-thread-M} au lieu d'en créer un par tâche, et qu'il faut l'arrêter avec {@code shutdown()}.
 *
 * Entrée : aucune. Trois tâches {@link Runnable} qui affichent le nom du thread qui les exécute.
 *
 * Sortie : une ligne par tâche avec le nom du thread du pool.
 *
 * Exemple de sortie attendue (l'ordre peut varier d'une exécution à l'autre) :
 * <pre>
 * Tâche 1 exécutée par : pool-1-thread-1
 * Tâche 2 exécutée par : pool-1-thread-2
 * Tâche 3 exécutée par : pool-1-thread-3
 * </pre>
 *
 * À observer : l'ordre des lignes peut changer. Sans {@code shutdown()}, le programme ne s'arrêterait jamais : les threads du pool restent en vie.
 *
 * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.executor.etape1_pool.FixedThreadPoolDemo}
 */
public class FixedThreadPoolDemo {

    public static void main(String[] args) {

        ExecutorService executor = Executors.newFixedThreadPool(3);

        try {
            for (int i = 1; i <= 10; i++) {
                int taskId = i;
                executor.submit(
                        () ->
                        System.out.println("Tâche " + taskId + " exécutée par : " + Thread.currentThread().getName())
                );
            }
        } finally {
            // Refuse les nouvelles tâches mais laisse finir celles déjà soumises.
            // Sans shutdown, les threads du pool empêchent la JVM de s'arrêter.
            executor.shutdown();
        }
    }
}

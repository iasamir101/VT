package fr.vt.simulation.threadclassique.executor.etape3_completablefuture;

import java.util.concurrent.CompletableFuture;

/**
 * Démo CompletableFuture.runAsync : lancer une tâche asynchrone sans résultat.
 *
 * But : montrer que la tâche s'exécute dans un autre thread (le ForkJoinPool commun) pendant que le thread principal continue.
 *
 * Entrée : aucune. Une tâche qui affiche quatre fois le nom de son thread.
 *
 * Sortie : une ligne du thread principal et quatre lignes de la tâche asynchrone (toujours le même thread).
 *
 * Exemple de sortie attendue (l'ordre peut varier d'une exécution à l'autre) :
 * <pre>
 * Thread principal
 * Exécution dans : ForkJoinPool.commonPool-worker-1
 * Exécution dans : ForkJoinPool.commonPool-worker-1
 * Exécution dans : ForkJoinPool.commonPool-worker-1
 * Exécution dans : ForkJoinPool.commonPool-worker-1
 * </pre>
 *
 * À observer : "Thread principal" peut apparaître avant ou après les lignes de la tâche : le thread principal n'attend la tâche qu'au {@code join()} final. Les quatre lignes de la tâche s'exécutent dans l'ordre, dans le même thread.
 *
 * Voir aussi : {@link CompletableFutureSupplyAsyncDemo} (avec un résultat).
 *
 * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.executor.etape3_completablefuture.CompletableFutureRunAsyncDemo}
 */
public class CompletableFutureRunAsyncDemo {

    public static void main(String[] args) {
        // Sans executor fourni, la tâche s'exécute dans le ForkJoinPool commun
        // (affiche "ForkJoinPool.commonPool-worker-N")
        CompletableFuture<Void> future = CompletableFuture.runAsync(
                () ->
                {
                System.out.println("Exécution dans : " + Thread.currentThread().getName());
                System.out.println("Exécution dans : " + Thread.currentThread().getName());
                System.out.println("Exécution dans : " + Thread.currentThread().getName());
                System.out.println("Exécution dans : " + Thread.currentThread().getName());
                }
        );

        // Le thread principal continue sans attendre : cette ligne peut s'afficher en premier
        System.out.println("Thread principal");

        // Attendre la fin : les threads du pool commun sont daemon,
        // la JVM pourrait s'arrêter avant que la tâche ne s'exécute
        future.join();
    }
}

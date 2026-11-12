package fr.vt.simulation.threadclassique.executor.etape3_completablefuture;

import java.util.concurrent.CompletableFuture;

/**
 * Démo CompletableFuture.runAsync : lancer une tâche asynchrone sans résultat.
 *
 * But : montrer que la tâche s'exécute dans un autre thread (le ForkJoinPool commun) pendant que le thread principal continue.
 *
 * Entrée : aucune. Une tâche qui affiche le nom de son thread.
 *
 * Sortie : une ligne du thread principal et une ligne de la tâche asynchrone.
 *
 * Exemple de sortie attendue (l'ordre peut varier d'une exécution à l'autre) :
 * <pre>
 * Thread principal
 * Exécution dans : ForkJoinPool.commonPool-worker-1
 * </pre>
 *
 * À observer : les deux lignes peuvent apparaître dans n'importe quel ordre : le thread principal n'attend la tâche qu'au {@code join()} final.
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

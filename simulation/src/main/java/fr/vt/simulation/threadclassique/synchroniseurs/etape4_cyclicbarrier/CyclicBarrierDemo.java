package fr.vt.simulation.threadclassique.synchroniseurs.etape4_cyclicbarrier;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * Démo CyclicBarrier : faire attendre plusieurs threads les uns les autres à chaque étape.
 *
 * But : montrer que tous les threads se retrouvent au même point avant de repartir ensemble,
 * et que la barrière se réinitialise toute seule pour l'étape suivante (elle est "cyclique").
 *
 * Entrée : aucune. Trois threads qui font quatre étapes de travail de durées différentes (1 s, 2 s et 3 s par étape).
 *
 * Sortie : une ligne par arrivée à la barrière, et une ligne quand la barrière est franchie.
 *
 * Exemple de sortie attendue (extrait : les deux premières des quatre étapes ; l'ordre des arrivées peut varier) :
 * <pre>
 * Thread 1 arrive à la barrière (étape 1)
 * Thread 2 arrive à la barrière (étape 1)
 * Thread 3 arrive à la barrière (étape 1)
 * >>> Tous arrivés : étape 1 terminée, on repart ensemble
 * Thread 1 arrive à la barrière (étape 2)
 * Thread 2 arrive à la barrière (étape 2)
 * Thread 3 arrive à la barrière (étape 2)
 * >>> Tous arrivés : étape 2 terminée, on repart ensemble
 * ...
 * </pre>
 *
 * À observer : aucun thread ne passe à l'étape 2 avant que le plus lent ait fini l'étape 1.
 * La ligne ">>>" est affichée une seule fois par étape, par le dernier thread arrivé.
 *
 * Différence avec CountDownLatch : ici ce sont les MÊMES threads qui s'attendent mutuellement, et la barrière
 * est réutilisable. Un latch est à usage unique et un thread peut attendre sans décompter.
 *
 * Voir aussi : {@code CountDownLatchDemo}.
 *
 * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.synchroniseurs.etape4_cyclicbarrier.CyclicBarrierDemo}
 */
public class CyclicBarrierDemo {

    private static final int THREADS = 3;
    private static final int STEPS = 4;

    public static void main(String[] args) {
        // Le Runnable est exécuté UNE fois par cycle, par le dernier thread arrivé, avant de libérer les autres
        int[] step = {1}; // numéro d'étape affiché par l'action de barrière (modifiée seulement par elle)
        CyclicBarrier barrier = new CyclicBarrier(THREADS, () ->
                System.out.println(">>> Tous arrivés : étape " + step[0]++ + " terminée, on repart ensemble"));

        for (int i = 1; i <= THREADS; i++) {
            int id = i;
            new Thread(() -> work(id, barrier)).start();
        }
    }

    private static void work(int id, CyclicBarrier barrier) {
        try {
            for (int step = 1; step <= STEPS; step++) {
                Thread.sleep(id * 1000L); // travail simulé : plus le numéro est grand, plus le thread est lent
                System.out.println("Thread " + id + " arrive à la barrière (étape " + step + ")");
                // await() bloque jusqu'à ce que THREADS threads l'aient appelé, puis la barrière se réinitialise
                barrier.await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (BrokenBarrierException e) {
            // Un autre thread a été interrompu ou a échoué : la barrière est cassée pour tout le monde
            System.out.println("Thread " + id + " : barrière cassée");
        }
    }
}

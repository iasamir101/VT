package fr.vt.simulation.threadclassique.synchroniseurs.etape2_countdownlatch;

import java.util.concurrent.CountDownLatch;

/**
 * Démo CountDownLatch : attendre que plusieurs threads aient fini leur travail.
 *
 * But : montrer qu'un thread (main) peut attendre sans {@code join()} que N événements se soient produits.
 *
 * Entrée : aucune. Trois ouvriers qui travaillent 1 s, 2 s et 3 s, et un compteur initialisé à 3.
 *
 * Sortie : une ligne à chaque fin d'ouvrier, puis une ligne quand main est libéré.
 *
 * Exemple de sortie attendue (l'ordre des ouvriers peut varier) :
 * <pre>
 * main attend les 3 ouvriers...
 * Ouvrier 1 a terminé
 * Ouvrier 2 a terminé
 * Ouvrier 3 a terminé
 * main libéré : tous les ouvriers ont fini
 * </pre>
 *
 * À observer : main reste bloqué sur {@code await()} environ 3 s, puis reprend d'un coup quand le compteur arrive à 0.
 * Le compteur ne se remet jamais à 0 : un CountDownLatch s'utilise UNE seule fois (pour réutiliser, voir CyclicBarrier).
 *
 * Voir aussi : {@code CyclicBarrierDemo} (attente mutuelle et réutilisable).
 *
 * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.synchroniseurs.etape2_countdownlatch.CountDownLatchDemo}
 */
public class CountDownLatchDemo {

    private static final int WORKERS = 3;

    public static void main(String[] args) throws InterruptedException {

        // Compteur initialisé au nombre d'événements à attendre
        CountDownLatch latch = new CountDownLatch(WORKERS);

        for (int i = 1; i <= WORKERS; i++) {
            int id = i;
            new Thread(() -> {
                try {
                    Thread.sleep(id * 1000L); // travail simulé : 1 s, 2 s, 3 s
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                // On affiche AVANT countDown() : après le dernier countDown(), main est libéré
                // et son message pourrait s'intercaler avant celui de l'ouvrier
                System.out.println("Ouvrier " + id + " a terminé");

                // countDown() décrémente le compteur et ne bloque jamais l'ouvrier
                latch.countDown();
            }).start();
        }

        System.out.println("main attend les " + WORKERS + " ouvriers...");

        // await() bloque main jusqu'à ce que le compteur atteigne 0
        latch.await();

        System.out.println("main libéré : tous les ouvriers ont fini");
    }
}

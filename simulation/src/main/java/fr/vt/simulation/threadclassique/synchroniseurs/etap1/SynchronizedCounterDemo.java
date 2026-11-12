package fr.vt.simulation.threadclassique.synchroniseurs.etap1;

import java.util.concurrent.Semaphore;

/**
 * Démo synchronized : le compteur de {@code RaceConditionDemo}, protégé par une méthode {@code synchronized}.
 *
 * But : montrer que l'exclusion mutuelle supprime la race condition : un seul thread à la fois exécute {@code counter++}.
 *
 * Entrée : aucune. Deux threads font chacun 1 000 000 appels à {@code increment()}.
 *
 * Sortie : une ligne qui compare la valeur attendue à la valeur obtenue.
 *
 * Exemple de sortie attendue (l'ordre peut varier d'une exécution à l'autre) :
 * <pre>
 * Attendu = 2000000, obtenu = 2000000
 * </pre>
 *
 * À observer : le résultat est toujours exact. Le prix à payer : les threads s'attendent, l'exécution est plus lente que la version non protégée.
 *
 * Voir aussi : {@link fr.vt.simulation.threadclassique.race.RaceConditionDemo} (version avec le bug).
 *
 * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.synchroniseurs.etap1.SynchronizedCounterDemo}
 */
public class SynchronizedCounterDemo {

    private static final int ITERATIONS = 1_000_000;

    private static int counter = 0;

    // static synchronized : le verrou est celui de la classe (SynchronizedCounterDemo.class).
    // Un seul thread à la fois exécute le corps, donc counter++ n'est plus interrompu.
    private static synchronized void increment() {
        counter++;
    }

    public static void main(String[] args) throws InterruptedException {
        Runnable task = () -> {
            for (int i = 0; i < ITERATIONS; i++) {
                increment();
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start();
        t2.start();
        // join() garantit aussi la visibilité : après join, main voit les écritures des threads
        t1.join();
        t2.join();

        System.out.println("Attendu = " + (2 * ITERATIONS) + ", obtenu = " + counter);
    }

    /**
     * Démo Semaphore : un parking de 3 places partagé par 10 voitures.
     *
     * But : montrer comment un {@link java.util.concurrent.Semaphore} limite le nombre de threads qui accèdent en même temps à une ressource.
     *
     * Entrée : aucune. 10 threads (voitures) veulent se garer 2 secondes dans un parking de 3 places (3 permis).
     *
     * Sortie : pour chaque voiture : "attend une place", "entre dans le parking", puis "quitte le parking".
     *
     * Exemple de sortie attendue (l'ordre peut varier d'une exécution à l'autre) :
     * <pre>
     * Voiture 6 attend une place
     * Voiture 7 attend une place
     * Voiture 10 attend une place
     * ...
     * Voiture 6 entre dans le parking
     * Voiture 7 entre dans le parking
     * Voiture 10 entre dans le parking
     * Voiture 6 quitte le parking
     * Voiture 3 entre dans le parking
     * ...
     * </pre>
     *
     * À observer : jamais plus de 3 voitures entre un "entre" et son "quitte". Une nouvelle voiture n'entre qu'après une sortie. Le programme dure environ 8 secondes (4 vagues de 2 s).
     *
     * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.synchroniseurs.etap1.SynchronizedCounterDemo.SemaphoreParkingDemo}
     */
    public static class SemaphoreParkingDemo {

        private static final int PLACES = 3;
        private static final int VOITURES = 10;

        public static void main(String[] args) {
            // Un permis = une place libre
            Semaphore parking = new Semaphore(PLACES);

            for (int i = 1; i <= VOITURES; i++) {
                final int carId = i;

                new Thread(() -> {
                    try {
                        System.out.println("Voiture " + carId + " attend une place");

                        // Prend un permis, ou bloque tant qu'aucune place n'est libre
                        parking.acquire();
                        try {
                            System.out.println("Voiture " + carId + " entre dans le parking");
                            Thread.sleep(2000); // stationnement
                            System.out.println("Voiture " + carId + " quitte le parking");
                        } finally {
                            // Rendre le permis dans finally : sinon une exception ferait perdre une place
                            parking.release();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        }
    }
}

package fr.vt.simulation.threadclassique.synchronisation;

import java.util.concurrent.Semaphore;

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
 * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.synchronisation.SemaphoreParkingDemo}
 */
public class SemaphoreParkingDemo {

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

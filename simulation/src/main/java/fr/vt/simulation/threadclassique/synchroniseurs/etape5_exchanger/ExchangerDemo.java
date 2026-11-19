package fr.vt.simulation.threadclassique.synchroniseurs.etape5_exchanger;

import java.util.concurrent.Exchanger;

/**
 * Démo Exchanger : deux threads s'échangent un objet en un point de rendez-vous.
 *
 * But : montrer que chaque thread donne sa donnée et reçoit celle de l'autre, en un seul appel
 * {@code exchange()} qui bloque jusqu'à l'arrivée du partenaire.
 *
 * Entrée : aucune. Un thread rapide (1 s) et un thread lent (3 s), chacun avec sa donnée.
 *
 * Sortie : ce que chaque thread a reçu.
 *
 * Exemple de sortie attendue (l'ordre des deux dernières lignes peut varier) :
 * <pre>
 * Rapide arrive au rendez-vous avec : données-rapide
 * Lent arrive au rendez-vous avec : données-lent
 * Rapide a reçu : données-lent
 * Lent a reçu : données-rapide
 * </pre>
 *
 * À observer : le thread rapide attend environ 2 s le thread lent. Les deux repartent en même temps,
 * chacun avec la donnée de l'autre.
 *
 * Cas d'usage typique : un thread remplit un tampon pendant que l'autre vide le précédent, puis ils échangent
 * leurs tampons (double buffering).
 *
 * Voir aussi : {@code CyclicBarrierDemo} (rendez-vous sans échange, pour N threads).
 *
 * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.synchroniseurs.etape5_exchanger.ExchangerDemo}
 */
public class ExchangerDemo {

    public static void main(String[] args) {
        // Un Exchanger est toujours partagé par EXACTEMENT deux threads
        Exchanger<String> exchanger = new Exchanger<>();

        new Thread(() -> meet("Rapide", 1000, exchanger)).start();
        new Thread(() -> meet("Lent", 3000, exchanger)).start();
    }

    private static void meet(String name, long workMillis, Exchanger<String> exchanger) {
        try {
            Thread.sleep(workMillis); // travail simulé avant le rendez-vous
            String mine = "données-" + name.toLowerCase();
            System.out.println(name + " arrive au rendez-vous avec : " + mine);

            // exchange() bloque jusqu'à l'arrivée de l'autre thread, puis chacun reçoit la donnée de l'autre
            String received = exchanger.exchange(mine);

            System.out.println(name + " a reçu : " + received);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

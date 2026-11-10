package fr.vt.simulation.threadclassique.synchronisation;

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
 * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.synchronisation.SynchronizedCounterDemo}
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
}

package fr.vt.simulation.threadclassique.race;


/**
 *
 * Une race condition survient lorsque plusieurs threads manipulent une même variable partagée en même temps,
 * et que le résultat final dépend de l'ordre imprévisible dans lequel le processeur exécute ces threads.
 *
 *
 */

import fr.vt.simulation.threadclassique.synchroniseurs.etape2_synchronized.SynchronizedCounterDemo;

/**
 * Démo race condition : deux threads incrémentent le même compteur sans aucune protection.
 *
 * But : montrer qu'une opération aussi simple que {@code counter++} n'est pas atomique et perd des mises à jour quand deux threads l'exécutent en parallèle.
 *
 * Entrée : aucune. Deux threads font chacun 1 000 000 incréments sur le champ statique {@code counter}.
 *
 * Sortie : une ligne qui compare la valeur attendue (2 000 000) à la valeur obtenue.
 *
 * Exemple de sortie attendue (l'ordre peut varier d'une exécution à l'autre) :
 * <pre>
 * Attendu = 2000000, obtenu = 1042884
 * </pre>
 *
 * À observer : la valeur obtenue est inférieure à 2 000 000 et change à chaque exécution : c'est le signe d'une race condition.
 *
 * Voir aussi : {@link SynchronizedCounterDemo} (version corrigée).
 *
 * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.race.RaceConditionDemo}
 */
public class RaceConditionDemo {

    // Assez d'itérations pour que les threads s'entrelacent vraiment
    // (avec 1 000, le premier thread finit souvent avant que le second démarre).
    private static final int ITERATIONS = 1_000_000;

    // Variable partagée, non protégée
    private static int counter = 0;

    public static void main(String[] args) throws InterruptedException {
        Runnable increment = () -> {
            for (int i = 0; i < ITERATIONS; i++) {
                // counter++ = lire, ajouter 1, écrire : trois étapes non atomiques.
                // Deux threads peuvent lire la même valeur et perdre un incrément.
                counter++;
            }
        };

        Thread t1 = new Thread(increment);
        Thread t2 = new Thread(increment);
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("Attendu = " + (2 * ITERATIONS) + ", obtenu = " + counter);
    }
}

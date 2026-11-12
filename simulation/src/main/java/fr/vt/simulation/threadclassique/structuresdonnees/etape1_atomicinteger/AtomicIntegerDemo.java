package fr.vt.simulation.threadclassique.structuresdonnees.etape1_atomicinteger;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Démo AtomicInteger : un compteur partagé sans perte, sans {@code synchronized}.
 *
 * But : montrer qu'un simple {@code int} perd des incréments quand deux threads l'incrémentent en même temps,
 * alors qu'un {@link AtomicInteger} n'en perd aucun.
 *
 * Entrée : aucune. Deux threads incrémentent chacun 100 000 fois le même compteur.
 *
 * Sortie : la valeur attendue et la valeur obtenue pour chaque type de compteur.
 *
 * Exemple de sortie attendue (la valeur fausse change à chaque exécution) :
 * <pre>
 * Attendu : 200000
 * int           : 117342
 * AtomicInteger : 200000
 * </pre>
 *
 * À observer : l'{@code int} est presque toujours inférieur à 200 000 (race condition),
 * l'{@code AtomicInteger} donne toujours 200 000.
 *
 * Principe : {@code incrementAndGet()} utilise une instruction du processeur, le Compare-And-Swap (CAS) :
 * "mettre la nouvelle valeur seulement si la valeur actuelle est celle que j'ai lue, sinon recommencer".
 * Aucun verrou n'est pris, aucun thread n'est mis en attente.
 * Voir aussi : {@code AtomicReferenceDemo} (le même principe sur un objet).
 *
 * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.structuresdonnees.etape1_atomicinteger.AtomicIntegerDemo}
 */
public class AtomicIntegerDemo {

    // Nombre d'incréments faits par CHAQUE thread (assez grand pour que les threads se chevauchent)
    private static final int PER_THREAD = 100000;

    // Compteur ordinaire : counter++ = lire, ajouter 1, écrire (3 étapes, pas une seule)
    private static int counter = 0;

    // Compteur atomique : la même opération en une seule étape indivisible
    private static final AtomicInteger atomicCounter = new AtomicInteger();

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(AtomicIntegerDemo::count);
        Thread t2 = new Thread(AtomicIntegerDemo::count);

        t1.start();
        t2.start();

        // join() : main attend les deux threads avant de lire les compteurs
        t1.join();
        t2.join();

        System.out.println("Attendu : " + 2 * PER_THREAD);
        System.out.println("int           : " + counter);
        System.out.println("AtomicInteger : " + atomicCounter.get());
    }

    private static void count() {
        for (int i = 0; i < PER_THREAD; i++) {
            // Race condition : les deux threads peuvent lire la même valeur, un incrément est alors perdu
            counter++;

            // Atomique : lecture, ajout et écriture ne peuvent pas être entrecoupés par l'autre thread
            atomicCounter.incrementAndGet();
        }
    }
}

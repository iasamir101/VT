package fr.vt.simulation.threadclassique.structuresdonnees.etape2_atomiclong;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Démo AtomicLong vs LongAdder : deux compteurs 64 bits sans verrou, comparés sous forte contention.
 *
 * But : montrer que les deux comptent juste, mais que {@link LongAdder} va beaucoup plus vite qu'{@link AtomicLong}
 * quand de nombreux threads incrémentent le même compteur en même temps.
 *
 * Entrée : aucune. 8 threads incrémentent chacun 2 000 000 de fois un compteur partagé :
 * une première fois un {@code AtomicLong}, une seconde fois un {@code LongAdder}. Chaque mesure est répétée 3 fois.
 *
 * Sortie : pour chaque essai, le total obtenu et la durée de chaque compteur.
 *
 * Exemple de sortie attendue (les durées dépendent de la machine) :
 * <pre>
 * 8 threads x 2000000 incréments, attendu = 16000000
 * Essai 1 : AtomicLong = 16000000 en 210 ms | LongAdder = 16000000 en 31 ms
 * Essai 2 : AtomicLong = 16000000 en 328 ms | LongAdder = 16000000 en 23 ms
 * Essai 3 : AtomicLong = 16000000 en 178 ms | LongAdder = 16000000 en 18 ms
 * </pre>
 *
 * À observer : les deux totaux sont toujours exacts (aucun incrément perdu), mais {@code LongAdder} est environ 5 à 10 fois plus rapide.
 * Les durées varient d'un essai à l'autre (charge de la machine, compilation JIT) : c'est l'écart entre les deux qui compte, pas les valeurs.
 *
 * Principe : {@code AtomicLong} est une seule case mémoire ; tous les threads font leur CAS dessus, la plupart échouent et
 * recommencent. {@code LongAdder} répartit les incréments sur plusieurs cellules (une par thread en conflit) et ne les
 * additionne qu'à l'appel de {@code sum()}.
 * Voir aussi : {@code AtomicIntegerDemo} (le même CAS sur 32 bits), {@code AtomicReferenceDemo} (le CAS sur un objet).
 *
 * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.structuresdonnees.etape2_atomiclong.AtomicLongDemo}
 */
public class AtomicLongDemo {

    private static final int THREADS = 8;
    private static final int PER_THREAD = 2_000_000;
    private static final int TRIALS = 3;

    public static void main(String[] args) throws InterruptedException {
        System.out.println(THREADS + " threads x " + PER_THREAD + " incréments, attendu = " + (long) THREADS * PER_THREAD);

        for (int trial = 1; trial <= TRIALS; trial++) {
            // AtomicLong : une seule valeur partagée, chaque incrément est un CAS sur la même case mémoire.
            AtomicLong atomic = new AtomicLong();
            long atomicMs = run(atomic::incrementAndGet);

            // LongAdder : une base + des cellules ; un thread en conflit écrit dans « sa » cellule au lieu de réessayer.
            LongAdder adder = new LongAdder();
            long adderMs = run(adder::increment);

            // get() est exact à tout instant ; sum() additionne les cellules et n'est exact
            // qu'une fois les écritures terminées (ici, après les join()).
            System.out.println("Essai " + trial + " : AtomicLong = " + atomic.get() + " en " + atomicMs + " ms"
                    + " | LongAdder = " + adder.sum() + " en " + adderMs + " ms");
        }
    }

    // Lance THREADS threads qui exécutent chacun PER_THREAD fois l'incrément, et renvoie la durée en ms.
    private static long run(Runnable increment) throws InterruptedException {
        Thread[] threads = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < PER_THREAD; j++) {
                    increment.run();
                }
            });
        }

        long start = System.nanoTime();
        for (Thread t : threads) {
            t.start();
        }
        // join() : main attend la fin de tous les threads et voit ensuite toutes leurs écritures.
        for (Thread t : threads) {
            t.join();
        }
        return (System.nanoTime() - start) / 1_000_000;
    }
}

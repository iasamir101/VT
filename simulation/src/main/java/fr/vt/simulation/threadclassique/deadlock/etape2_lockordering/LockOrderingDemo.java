package fr.vt.simulation.threadclassique.deadlock.etape2_lockordering;

/**
 * Démo correction du deadlock par ordre global des verrous : quel que soit le sens du virement,
 * on verrouille toujours le compte d'identifiant le plus petit en premier.
 *
 * But : montrer qu'en imposant un ordre unique d'acquisition des verrous, l'attente circulaire devient impossible,
 * donc le deadlock aussi.
 *
 * Entrée : aucune. Deux comptes A (id 1) et B (id 2), 1 000 chacun. Le thread « virement A->B » fait 10 000 virements de 1
 * de A vers B, le thread « virement B->A » fait 10 000 virements de 1 de B vers A, en même temps.
 *
 * Sortie : la durée, les soldes finaux et le total, qui doit rester égal à 2 000.
 *
 * Exemple de sortie attendue (la durée varie) :
 * <pre>
 * 20000 virements terminés en 5 ms
 * A = 1000, B = 1000, total = 2000 (attendu 2000)
 * </pre>
 *
 * À observer : le programme se termine toujours, malgré des virements dans les deux sens qui se croisent sans cesse.
 * Les deux threads prennent d'abord le verrou de A : celui qui l'obtient prend ensuite B, l'autre attend sans rien détenir.
 *
 * Voir aussi : {@code DeadlockDemo} (le problème), {@code TryLockDemo} (autre correction, par timeout).
 *
 * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.deadlock.etape2_lockordering.LockOrderingDemo}
 */
public class LockOrderingDemo {

    private static final int TRANSFERS = 10_000;

    public static void main(String[] args) throws InterruptedException {
        Account a = new Account(1, "A", 1_000);
        Account b = new Account(2, "B", 1_000);

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < TRANSFERS; i++) {
                transfer(a, b, 1);
            }
        }, "virement A->B");
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < TRANSFERS; i++) {
                transfer(b, a, 1);
            }
        }, "virement B->A");

        long start = System.nanoTime();
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        long ms = (System.nanoTime() - start) / 1_000_000;

        System.out.println((2 * TRANSFERS) + " virements terminés en " + ms + " ms");
        // join() garantit que main voit les dernières écritures des deux threads (happens-before).
        System.out.println("A = " + a.balance() + ", B = " + b.balance()
                + ", total = " + (a.balance() + b.balance()) + " (attendu 2000)");
    }

    static void transfer(Account from, Account to, int amount) {
        // Ordre global : le plus petit id d'abord, quel que soit le sens du virement.
        // Les deux threads demandent donc les verrous dans le même ordre : pas de cycle possible.
        Account first = from.id() < to.id() ? from : to;
        Account second = (first == from) ? to : from;

        synchronized (first) {
            synchronized (second) {
                from.withdraw(amount);
                to.deposit(amount);
            }
        }
    }
}

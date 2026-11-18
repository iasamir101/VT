package fr.vt.simulation.threadclassique.deadlock.etape3_trylock;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Démo correction du deadlock par timeout : chaque virement tente de prendre les deux verrous avec {@code tryLock(délai)} ;
 * en cas d'échec, il relâche ce qu'il détient, attend un délai aléatoire et recommence.
 *
 * But : montrer qu'en renonçant à un verrou au lieu de l'attendre pour toujours, on casse la condition « détention et attente » :
 * le deadlock ne peut pas durer, même si les verrous sont pris dans l'ordre inverse.
 *
 * Entrée : aucune. Deux comptes A et B (1 000 chacun). Le thread « virement A->B » fait 5 virements de 100 de A vers B
 * (verrouille A puis B), le thread « virement B->A » fait 5 virements de 100 de B vers A (verrouille B puis A).
 * Une pause entre les deux verrous provoque volontairement le croisement, comme dans {@code DeadlockDemo}.
 *
 * Sortie : les échecs de {@code tryLock} (avec retour arrière), chaque virement réussi, puis les soldes et le total (2 000 attendu).
 *
 * Exemple de sortie attendue (l'ordre et le nombre d'échecs varient) :
 * <pre>
 * [virement A->B] échec sur Compte B, je relâche Compte A et je réessaie
 * [virement B->A] échec sur Compte A, je relâche Compte B et je réessaie
 * [virement B->A] échec sur Compte A, je relâche Compte B et je réessaie
 * [virement A->B] échec sur Compte B, je relâche Compte A et je réessaie
 * [virement B->A] échec sur Compte A, je relâche Compte B et je réessaie
 * [virement A->B] virement 1 effectué (tentative 3)
 * [virement A->B] échec sur Compte B, je relâche Compte A et je réessaie
 * [virement B->A] virement 1 effectué (tentative 4)
 * [virement B->A] virement 2 effectué (tentative 1)
 * ...
 * [virement B->A] virement 5 effectué (tentative 2)
 * [virement A->B] virement 5 effectué (tentative 2)
 * Tous les virements sont terminés en 20 tentatives (10 virements)
 * A = 1000, B = 1000, total = 2000 (attendu 2000)
 * </pre>
 *
 * À observer : il y a des échecs (le croisement se produit bien), mais le programme se termine toujours.
 * Le délai aléatoire avant de réessayer évite le livelock : sans lui, les deux threads pourraient échouer et recommencer
 * en même temps, indéfiniment.
 *
 * Voir aussi : {@code DeadlockDemo} (le problème), {@code LockOrderingDemo} (correction par ordre global, plus simple).
 *
 * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.deadlock.etape3_trylock.TryLockDemo}
 */
public class TryLockDemo {

    private static final int TRANSFERS = 5;
    private static final int AMOUNT = 100;
    private static final long LOCK_TIMEOUT_MS = 50;

    public static void main(String[] args) throws InterruptedException {

        Account a = new Account("A", 1_000);
        Account b = new Account("B", 1_000);

        int[] attempts = new int[2];

        Thread t1 = new Thread(() -> attempts[0] = runTransfers(a, b), "virement A->B");
        Thread t2 = new Thread(() -> attempts[1] = runTransfers(b, a), "virement B->A");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("Tous les virements sont terminés en " + (attempts[0] + attempts[1])
                + " tentatives (" + (2 * TRANSFERS) + " virements)");
        System.out.println("A = " + a.balance() + ", B = " + b.balance()
                + ", total = " + (a.balance() + b.balance()) + " (attendu 2000)");
    }

    // Renvoie le nombre total de tentatives faites par ce thread.
    private static int runTransfers(Account from, Account to) {
        int total = 0;
        try {
            for (int i = 1; i <= TRANSFERS; i++) {
                int attempt = 0;
                boolean done = false;
                while (!done) {
                    attempt++;
                    done = tryTransfer(from, to, AMOUNT);
                    if (!done) {
                        // Délai aléatoire : les deux threads ne réessaient pas au même instant (anti-livelock).
                        Thread.sleep(ThreadLocalRandom.current().nextLong(10, 60));
                    }
                }
                total += attempt;
                System.out.println("[" + Thread.currentThread().getName() + "] virement " + i
                        + " effectué (tentative " + attempt + ")");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return total;
    }

    // Renvoie true si le virement a eu lieu, false si un verrou n'a pas pu être obtenu à temps.
    private static boolean tryTransfer(Account from, Account to, int amount) throws InterruptedException {
        String me = Thread.currentThread().getName();
        // tryLock(délai) : attend au plus LOCK_TIMEOUT_MS, puis abandonne au lieu de bloquer pour toujours.
        if (!from.lock().tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            System.out.println("[" + me + "] échec sur " + from + ", je réessaie");
            return false;
        }
        try {
            // Même pause que dans DeadlockDemo : force l'autre thread à prendre son premier verrou.
            Thread.sleep(20);
            if (!to.lock().tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                // On rend le premier verrou (dans le finally) : c'est ce qui casse « détention et attente ».
                System.out.println("[" + me + "] échec sur " + to + ", je relâche " + from + " et je réessaie");
                return false;
            }
            try {
                from.withdraw(amount);
                to.deposit(amount);
                return true;
            } finally {
                to.lock().unlock();
            }
        } finally {
            // unlock() toujours dans un finally : sinon un verrou resterait pris pour toujours.
            from.lock().unlock();
        }
    }
}

package fr.vt.simulation.threadclassique.deadlock.etape1_deadlock;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/**
 * Démo deadlock : deux virements croisés verrouillent les mêmes comptes dans l'ordre inverse et s'attendent pour toujours.
 *
 * Un deadlock (interblocage) survient quand chaque thread détient un verrou et attend celui que détient l'autre :
 * aucun ne peut avancer, aucun ne relâche le sien.
 *
 * But : provoquer un deadlock à coup sûr, puis le détecter depuis le programme avec {@link ThreadMXBean#findDeadlockedThreads()}.
 *
 * Entrée : aucune. Deux comptes A et B (1 000 chacun). Le thread « virement A->B » verrouille A puis B,
 * le thread « virement B->A » verrouille B puis A.
 *
 * Sortie : les étapes de verrouillage de chaque thread, puis le rapport de détection (qui attend quel verrou, détenu par qui).
 *
 * Exemple de sortie attendue (l'ordre des deux premières lignes peut varier) :
 * <pre>
 * [virement A->B] verrouille Compte A
 * [virement B->A] verrouille Compte B
 * [virement A->B] attend Compte B...
 * [virement B->A] attend Compte A...
 * Deadlock détecté : 2 threads bloqués
 *  - virement A->B (BLOCKED) attend Compte B détenu par virement B->A
 *  - virement B->A (BLOCKED) attend Compte A détenu par virement A->B
 * Aucun virement n'a abouti : A = 1000, B = 1000
 * </pre>
 *
 * À observer : les deux threads sont BLOCKED pour toujours et chacun attend le verrou de l'autre (attente circulaire).
 * Sans détection, le programme ne s'arrêterait jamais et ne consommerait aucun CPU.
 * En dehors du programme, {@code jstack <pid>} (ou {@code jcmd <pid> Thread.print}) affiche « Found one Java-level deadlock ».
 *
 * Voir aussi : {@code LockOrderingDemo} (correction par ordre global) et {@code TryLockDemo} (correction par timeout).
 *
 * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.deadlock.etape1_deadlock.DeadlockDemo}
 */
public class DeadlockDemo {

    private static final int AMOUNT = 100;

    public static void main(String[] args) throws InterruptedException {

        Account a = new Account("A", 1_000);
        Account b = new Account("B", 1_000);

        // Ordre inverse : c'est ce qui crée l'attente circulaire.
        Thread t1 = new Thread(() -> transfer(a, b, AMOUNT), "virement A->B");
        Thread t2 = new Thread(() -> transfer(b, a, AMOUNT), "virement B->A");

        // Daemon : un thread bloqué sur un moniteur ne peut être ni interrompu ni tué ;
        // en daemon, il n'empêche pas la JVM de s'arrêter à la fin de main.
        t1.setDaemon(true);
        t2.setDaemon(true);
        t1.start();
        t2.start();

        // On laisse le temps aux deux threads de se bloquer mutuellement.
        Thread.sleep(1_000);

        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        // Renvoie les ids des threads engagés dans un cycle d'attente de verrous (null s'il n'y en a pas).
        long[] ids = mx.findDeadlockedThreads();
        if (ids == null) {
            System.out.println("Pas de deadlock cette fois-ci");
            return;
        }

        System.out.println("Deadlock détecté : " + ids.length + " threads bloqués");
        for (ThreadInfo info : mx.getThreadInfo(ids)) {
            System.out.println(" - " + info.getThreadName() + " (" + info.getThreadState() + ") attend "
                    + lockLabel(info.getLockName(), a, b) + " détenu par " + info.getLockOwnerName());
        }
        // Lecture sans verrou : acceptable ici car plus aucun thread ne modifie les soldes.
        System.out.println("Aucun virement n'a abouti : A = " + a.balance() + ", B = " + b.balance());

        // Avec exec:java, main tourne dans la JVM de Maven, qui ne s'arrête pas à la fin de main
        // et dont le plugin tente de nettoyer les threads restants : ceux-ci ne finiront jamais.
        // On arrête donc la JVM explicitement.
       System.exit(0);
    }

    private static void transfer(Account from, Account to, int amount) {
        String me = Thread.currentThread().getName();
        synchronized (from) {
            System.out.println("[" + me + "] verrouille " + from);
            // Pause pour que l'autre thread ait le temps de prendre son premier verrou :
            // sans elle, le deadlock serait possible mais rare.
            sleepQuietly(100);
            System.out.println("[" + me + "] attend " + to + "...");
            synchronized (to) {
                // Jamais atteint : l'autre thread détient « to » et attend « from ».
                from.withdraw(amount);
                to.deposit(amount);
                System.out.println("[" + me + "] virement effectué");
            }
        }
    }

    // getLockName() renvoie "classe@hashIdentité" (pas toString()) : on retrouve le compte correspondant
    // pour afficher « Compte A » plutôt que « ...Account@1b6d3586 ».
    private static String lockLabel(String lockName, Account... accounts) {
        for (Account account : accounts) {
            String id = account.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(account));
            if (id.equals(lockName)) {
                return account.toString();
            }
        }
        return lockName;
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

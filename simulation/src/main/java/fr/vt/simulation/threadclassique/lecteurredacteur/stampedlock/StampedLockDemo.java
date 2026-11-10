package fr.vt.simulation.threadclassique.lecteurredacteur.stampedlock;

import java.util.List;

/**
 * Démo lecteurs / rédacteurs avec {@link java.util.concurrent.locks.StampedLock} et lecture optimiste.
 *
 * But : montrer qu'une lecture peut se faire sans verrou, puis être validée, et refaite sous verrou seulement si une écriture a eu lieu entre-temps.
 *
 * Entrée : aucune. Un compte démarre à 1000 ; 3 lecteurs consultent le solde 5 fois ; 2 rédacteurs déposent 5 fois 100.
 *
 * Sortie : chaque consultation et chaque dépôt, puis le solde final.
 *
 * Exemple de sortie attendue (l'ordre peut varier d'une exécution à l'autre) :
 * <pre>
 * Reader-3 consulte le solde : 1000.0
 * Writer-1 dépôt : 100.0 | Solde : 1100.0
 * Writer-1 dépôt : 100.0 | Solde : 1200.0
 * ...
 * Writer-2 dépôt : 100.0 | Solde : 2000.0
 * Reader-3 consulte le solde : 2000.0
 * ...
 * Solde final : 2000.0
 * </pre>
 *
 * À observer : le solde final vaut toujours 1000 + 2 x 5 x 100 = 2000. Un lecteur ne voit jamais une valeur incohérente, même sans prendre de verrou la plupart du temps.
 *
 * Voir aussi : {@code lecteurredacteur.readwritelock} (verrou lecture/écriture classique).
 *
 * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.lecteurredacteur.stampedlock.StampedLockDemo}
 */
public class StampedLockDemo {

    public static void main(String[] args) throws InterruptedException {
        BankAccount account = new BankAccount();

        List<Thread> threads = List.of(
                new BalanceReader(account, "Reader-1"),
                new BalanceReader(account, "Reader-2"),
                new BalanceReader(account, "Reader-3"),
                new DepositWriter(account, "Writer-1"),
                new DepositWriter(account, "Writer-2"));

        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        System.out.println("Solde final : " + account.getBalance());
    }
}

package fr.vt.simulation.threadclassique.lecteurredacteur.stampedlock;

/**
 * Thread lecteur de la démo StampedLock.
 *
 * But : consulter le solde en lecture optimiste.
 *
 * Entrée : le compte partagé et le nom du thread.
 *
 * Sortie : 5 lignes "<nom> consulte le solde : <solde>".
 */
class BalanceReader extends Thread {

    private final BankAccount account;

    BalanceReader(BankAccount account, String name) {
        super(name);
        this.account = account;
    }

    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println(getName() + " consulte le solde : " + account.getBalanceOptimistic());
        }
    }
}

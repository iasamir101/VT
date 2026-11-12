package fr.vt.simulation.threadclassique.lecteurredacteur.etape2_stampedlock;

/**
 * Thread rédacteur de la démo StampedLock.
 *
 * But : déposer de l'argent sur le compte.
 *
 * Entrée : le compte partagé et le nom du thread.
 *
 * Sortie : 5 dépôts de 100, chacun affiché avec le nouveau solde.
 */
class DepositWriter extends Thread {

    private final BankAccount account;

    DepositWriter(BankAccount account, String name) {
        super(name);
        this.account = account;
    }

    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            account.deposit(100);
        }
    }
}

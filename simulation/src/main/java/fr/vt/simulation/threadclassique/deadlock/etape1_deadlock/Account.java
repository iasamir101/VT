package fr.vt.simulation.threadclassique.deadlock.etape1_deadlock;

/**
 * Compte bancaire minimal, utilisé comme verrou (moniteur) par {@link DeadlockDemo}.
 *
 * But : porter un solde et un nom lisible dans les traces de détection du deadlock.
 *
 * Entrée : un nom et un solde initial.
 *
 * Sortie : aucune (les accès au solde sont protégés par l'appelant, qui verrouille le compte avec {@code synchronized}).
 */
class Account {

    private final String name;
    private int balance;

    Account(String name, int balance) {
        this.name = name;
        this.balance = balance;
    }

    String name() {
        return name;
    }

    int balance() {
        return balance;
    }

    void withdraw(int amount) {
        balance -= amount;
    }

    void deposit(int amount) {
        balance += amount;
    }

    // Nom lisible dans les messages de la démo.
    @Override
    public String toString() {
        return "Compte " + name;
    }
}

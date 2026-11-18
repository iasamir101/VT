package fr.vt.simulation.threadclassique.deadlock.etape2_lockordering;

/**
 * Compte bancaire avec un identifiant unique, qui sert à fixer l'ordre global des verrous dans {@link LockOrderingDemo}.
 *
 * But : porter un solde et un {@code id} comparable ; le compte sert lui-même de verrou ({@code synchronized}).
 *
 * Entrée : un identifiant, un nom et un solde initial.
 *
 * Sortie : aucune (les accès au solde sont protégés par l'appelant).
 */
class Account {

    private final int id;
    private final String name;
    private int balance;

    Account(int id, String name, int balance) {
        this.id = id;
        this.name = name;
        this.balance = balance;
    }

    int id() {
        return id;
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

    @Override
    public String toString() {
        return "Compte " + name;
    }
}

package fr.vt.simulation.threadclassique.deadlock.etape3_trylock;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Compte bancaire qui porte son propre {@link ReentrantLock}, utilisé par {@link TryLockDemo}.
 *
 * But : permettre de tenter un verrouillage avec timeout ({@code tryLock}) au lieu d'attendre indéfiniment comme avec {@code synchronized}.
 *
 * Entrée : un nom et un solde initial.
 *
 * Sortie : aucune (les accès au solde sont protégés par l'appelant, qui détient {@link #lock()}).
 */
class Account {

    private final String name;
    private final ReentrantLock lock = new ReentrantLock();
    private int balance;

    Account(String name, int balance) {
        this.name = name;
        this.balance = balance;
    }

    ReentrantLock lock() {
        return lock;
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

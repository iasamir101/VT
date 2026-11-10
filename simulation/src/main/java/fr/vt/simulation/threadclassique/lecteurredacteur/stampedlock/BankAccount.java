package fr.vt.simulation.threadclassique.lecteurredacteur.stampedlock;

import java.util.concurrent.locks.StampedLock;

/**
 * Compte bancaire de la démo StampedLock.
 *
 * But : illustrer les trois modes du {@link StampedLock} : lecture optimiste, lecture verrouillée, écriture exclusive.
 *
 * Entrée : {@code deposit(double)} ajoute un montant au solde (initialement 1000).
 *
 * Sortie : {@code getBalanceOptimistic()} et {@code getBalance()} renvoient le solde ; chaque dépôt est affiché.
 */
class BankAccount {

    private final StampedLock lock = new StampedLock();

    private double balance = 1000;

    /** Lecture optimiste : aucun verrou si aucune écriture n'a eu lieu pendant la lecture. */
    double getBalanceOptimistic() {
        // Renvoie un "stamp" (numéro de version) sans bloquer personne
        long stamp = lock.tryOptimisticRead();
        double currentBalance = balance;

        // validate() est faux si un writeLock a été pris depuis tryOptimisticRead() :
        // la valeur lue peut être incohérente, on relit sous un vrai verrou de lecture
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                currentBalance = balance;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return currentBalance;
    }

    /** Lecture classique sous verrou de lecture (partagé). */
    double getBalance() {
        long stamp = lock.readLock();
        try {
            return balance;
        } finally {
            // Contrairement à ReentrantReadWriteLock, on libère avec le stamp obtenu
            lock.unlockRead(stamp);
        }
    }

    /** Écriture sous verrou exclusif. */
    void deposit(double amount) {
        long stamp = lock.writeLock();
        try {
            balance += amount;
            System.out.println(Thread.currentThread().getName()
                    + " dépôt : " + amount + " | Solde : " + balance);
        } finally {
            lock.unlockWrite(stamp);
        }
    }
}

package fr.vt.simulation.threadclassique.lecteurredacteur.readwritelock;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Donnée partagée de la démo lecteurs / rédacteurs, protégée par un {@link ReentrantReadWriteLock}.
 *
 * But : autoriser plusieurs lectures simultanées mais une seule écriture à la fois.
 *
 * Entrée : {@code write(String)} remplace la valeur.
 *
 * Sortie : {@code read()} affiche la valeur courante ; chaque opération dure 1 seconde.
 */
class SharedData {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private String data = "Valeur initiale";

    void read() {
        // Verrou partagé : les lecteurs ne se bloquent pas entre eux,
        // seulement si un rédacteur détient le verrou d'écriture
        lock.readLock().lock();
        try {
            System.out.println(Thread.currentThread().getName() + " lit : " + data);
            Thread.sleep(1000); // lecture longue simulée : on voit les lectures se chevaucher
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.readLock().unlock();
        }
    }

    void write(String newData) {
        // Verrou exclusif : attend que tous les lecteurs et rédacteurs en cours aient terminé
        lock.writeLock().lock();
        try {
            System.out.println(Thread.currentThread().getName() + " écrit : " + newData);
            data = newData;
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.writeLock().unlock();
        }
    }
}

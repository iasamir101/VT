package fr.vt.simulation.threadclassique.producteurconsommateur.etape1_waitnotify;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Tampon borné partagé entre producteur et consommateur, écrit avec le moniteur de l'objet.
 *
 * But : stocker au plus 5 entiers et bloquer le producteur quand il est plein, le consommateur quand il est vide.
 *
 * Entrée : {@code produce(int)} ajoute une valeur.
 *
 * Sortie : {@code consume()} retire et renvoie la plus ancienne valeur ; chaque opération et chaque attente est affichée.
 */
class BoundedBuffer {

    private static final int CAPACITY = 5;

    private final Queue<Integer> queue = new LinkedList<>();

    synchronized void produce(int value) throws InterruptedException {

        // while et non if : au réveil, la condition peut être redevenue fausse
        // (autre thread passé avant, ou réveil intempestif)
        while (queue.size() == CAPACITY) {
            System.out.println("Buffer plein : producteur en attente");
            wait(); // relâche le verrou et dort jusqu'à un notify
        }
        queue.add(value);
        System.out.println("Produit : " + value);
        // Réveille le consommateur qui attend peut-être un élément
        notifyAll();
    }

    synchronized int consume() throws InterruptedException {

        while (queue.isEmpty()) {
            System.out.println("Buffer vide : consommateur en attente");
            wait();
        }
        int value = queue.poll();
        System.out.println("Consommé : " + value);
        // Réveille le producteur qui attend peut-être une place libre
        notifyAll();
        return value;
    }
}

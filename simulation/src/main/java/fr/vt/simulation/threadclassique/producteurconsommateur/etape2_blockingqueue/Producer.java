package fr.vt.simulation.threadclassique.producteurconsommateur.etape2_blockingqueue;

import java.util.concurrent.BlockingQueue;

/**
 * Thread producteur de la démo BlockingQueue.
 *
 * But : produire 10 éléments puis signaler la fin.
 *
 * Entrée : la file bloquante partagée.
 *
 * Sortie : les valeurs 1 à 10 puis la poison pill ({@code -1}) dans la file ; un message par élément produit.
 */
class Producer extends Thread {

    private final BlockingQueue<Integer> queue;

    Producer(BlockingQueue<Integer> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            for (int i = 1; i <= 10; i++) {
                // put() bloque si la file est pleine : c'est la contre-pression
                queue.put(i);
                System.out.println("Producteur : élément produit = " + i);
                Thread.sleep(500);
            }
            // Valeur spéciale qui indique au consommateur qu'il n'y aura plus rien
            queue.put(BlockingQueueProducerConsumerDemo.POISON_PILL);
            System.out.println("Producteur : terminé");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

package fr.vt.simulation.threadclassique.producteurconsommateur.etape2_blockingqueue;

import java.util.concurrent.BlockingQueue;

/**
 * Thread consommateur de la démo BlockingQueue.
 *
 * But : consommer les éléments jusqu'à recevoir la poison pill.
 *
 * Entrée : la file bloquante partagée.
 *
 * Sortie : un message par élément consommé, puis "Consommateur : terminé".
 */
class Consumer extends Thread {

    private final BlockingQueue<Integer> queue;

    Consumer(BlockingQueue<Integer> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            while (true) {
                // take() bloque si la file est vide
                int value = queue.take();
                if (value == BlockingQueueProducerConsumerDemo.POISON_PILL) {
                    break;
                }
                System.out.println("Consommateur : élément consommé = " + value);
                Thread.sleep(1000);
            }
            System.out.println("Consommateur : terminé");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

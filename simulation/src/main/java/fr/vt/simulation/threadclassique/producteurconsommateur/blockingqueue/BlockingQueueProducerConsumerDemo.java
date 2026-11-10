package fr.vt.simulation.threadclassique.producteurconsommateur.blockingqueue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Démo producteur / consommateur avec une {@link BlockingQueue}.
 *
 * But : montrer la solution standard : la file bloquante gère seule l'attente, et une "poison pill" ({@code -1}) signale la fin.
 *
 * Entrée : aucune. Un producteur envoie 1 à 10 (toutes les 500 ms) dans une {@link ArrayBlockingQueue} de capacité 5, puis la poison pill ; un consommateur lit toutes les 1000 ms.
 *
 * Sortie : chaque élément produit et consommé, puis les messages de fin des deux threads.
 *
 * Exemple de sortie attendue (l'ordre peut varier d'une exécution à l'autre) :
 * <pre>
 * Producteur : élément produit = 1
 * Consommateur : élément consommé = 1
 * Producteur : élément produit = 2
 * Consommateur : élément consommé = 2
 * Producteur : élément produit = 3
 * Producteur : élément produit = 4
 * ...
 * Producteur : terminé
 * ...
 * Consommateur : terminé
 * Application terminée
 * </pre>
 *
 * À observer : aucun wait/notify dans le code. Le consommateur s'arrête proprement à la réception de {@code -1}, sans être interrompu.
 *
 * Voir aussi : {@code producteurconsommateur.waitnotify} (version écrite à la main).
 *
 * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.producteurconsommateur.blockingqueue.BlockingQueueProducerConsumerDemo}
 */
public class BlockingQueueProducerConsumerDemo {

    /** Signal de fin envoyé par le producteur. */
    static final int POISON_PILL = -1;

    public static void main(String[] args) throws InterruptedException {
        // File bornée à 5 éléments : le producteur est freiné si le consommateur prend du retard
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(5);

        Thread producer = new Thread(new Producer(queue));
        Thread consumer = new Thread(new Consumer(queue));
        producer.start();
        consumer.start();

        producer.join();
        consumer.join();
        System.out.println("Application terminée");
    }
}

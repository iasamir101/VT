package fr.vt.simulation.threadclassique.producteurconsommateur.etape1_waitnotify;

/**
 * Démo producteur / consommateur "à la main" avec {@code wait()} et {@code notifyAll()}.
 *
 * But : montrer le fonctionnement interne d'un tampon borné : le producteur attend quand le tampon est plein, le consommateur attend quand il est vide.
 *
 * Entrée : aucune. Un producteur produit 10 entiers (un toutes les 500 ms) dans un tampon de 5 places ; un consommateur les lit (un toutes les 1000 ms).
 *
 * Sortie : chaque production et chaque consommation, plus les messages d'attente quand le tampon est plein ou vide.
 *
 * Exemple de sortie attendue (l'ordre peut varier d'une exécution à l'autre) :
 * <pre>
 * Produit : 1
 * Consommé : 1
 * Produit : 2
 * Consommé : 2
 * Produit : 3
 * Produit : 4
 * Produit : 5
 * Consommé : 3
 * ...
 * Application terminée
 * </pre>
 *
 * À observer : le consommateur étant deux fois plus lent, les "Produit" prennent de l'avance. Les 10 valeurs sont consommées dans l'ordre, aucune n'est perdue.
 *
 * Voir aussi : {@code producteurconsommateur.etape2_blockingqueue} (même problème avec une BlockingQueue).
 *
 * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.producteurconsommateur.etape1_waitnotify.WaitNotifyProducerConsumerDemo}
 */
public class WaitNotifyProducerConsumerDemo {

    private static final int ELEMENTS = 10;

    public static void main(String[] args) throws InterruptedException {

        BoundedBuffer buffer = new BoundedBuffer();

        Producer producer = new Producer(buffer, ELEMENTS);
        Consumer consumer = new Consumer(buffer, ELEMENTS);

        producer.start();
        consumer.start();

        producer.join();
        consumer.join();

        System.out.println("Application terminée");
    }
}

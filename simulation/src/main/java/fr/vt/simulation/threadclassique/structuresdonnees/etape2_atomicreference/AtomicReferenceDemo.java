package fr.vt.simulation.threadclassique.structuresdonnees.etape2_atomicreference;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Démo AtomicReference : Compare-And-Swap (CAS) sur une référence.
 *
 * But : montrer qu'avec une écriture ordinaire la dernière valeur écrite l'emporte (race condition),
 * alors que {@code compareAndSet(attendu, nouveau)} ne réussit que pour un seul thread.
 *
 * Entrée : aucune. 1. Deux threads écrivent "B" et "C" dans la même variable ;
 * 2. deux threads tentent de passer un statut de "EN_ATTENTE" à "VALIDE" ou à "REFUSE".
 *
 * Sortie : la valeur finale de la variable, le résultat (true / false) de chaque compareAndSet, puis le statut final.
 *
 * Exemple de sortie attendue (les valeurs changent d'une exécution à l'autre) :
 * <pre>
 * C
 * Thread 2 : false
 * Thread 1 : true
 * Statut final : VALIDE
 * </pre>
 *
 * À observer : la première valeur est "B" ou "C" selon l'ordre d'écriture. Pour le statut, un seul compareAndSet
 * renvoie true : l'autre trouve que le statut n'est plus "EN_ATTENTE" et ne change rien.
 *
 * Voir aussi : {@code AtomicIntegerDemo} (le même principe sur un entier).
 *
 * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.structuresdonnees.etape2_atomicreference.AtomicReferenceDemo}
 */
public class AtomicReferenceDemo {
    private static String valeur = "A";
    private static AtomicReference<String> statut = new AtomicReference<>("EN_ATTENTE");

    public static void main(String[] args) throws InterruptedException {

        Thread ta = new Thread(() -> valeur = "C");
        Thread tb = new Thread(() -> valeur = "B");
        ta.start();
        tb.start();
        ta.join();
        tb.join();

        System.out.println(valeur); // Affiche "B" ou "C" (selon quel thread écrit en dernier - Race condition)
        // Le résultat final dépend de l'ordre imprévisible :
        // les deux threads ont modifié la variable, et la dernière valeur écrite est affichée

        Thread t1 = new Thread(() -> {
            boolean success = statut.compareAndSet("EN_ATTENTE", "VALIDE");
            System.out.println("Thread 1 : " + success);
        });

        Thread t2 = new Thread(() -> {
            boolean success = statut.compareAndSet("EN_ATTENTE", "REFUSE");
            System.out.println("Thread 2 : " + success);
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("Statut final : " + statut.get()); // --> Affiche "VALIDE" ou "REFUSE" (selon quel thread gagne la compétition du CAS)
        // Un des deux threads ne peut pas modifier la variable.
    }
}

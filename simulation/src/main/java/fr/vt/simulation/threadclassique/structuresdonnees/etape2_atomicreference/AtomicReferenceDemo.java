package fr.vt.simulation.threadclassique.structuresdonnees.etape2_atomicreference;


import java.util.concurrent.atomic.AtomicReference;

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
        //Le résultat final dépend de l'ordre imprévisible
        // les deux thread ont modifie les variable le dernière valeur est affiché


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
        // Un de deux thread ne peut pas modifier la variable.
    }
}

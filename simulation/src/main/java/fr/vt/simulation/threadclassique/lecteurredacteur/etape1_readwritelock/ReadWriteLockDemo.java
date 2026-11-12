package fr.vt.simulation.threadclassique.lecteurredacteur.etape1_readwritelock;

import java.util.List;

/**
 * Démo lecteurs / rédacteurs avec {@link java.util.concurrent.locks.ReentrantReadWriteLock}.
 *
 * But : montrer que plusieurs lecteurs lisent en même temps, alors qu'un rédacteur écrit seul.
 *
 * Entrée : aucune. 3 lecteurs lisent 3 fois une chaîne partagée, 2 rédacteurs l'écrivent 3 fois ; chaque opération dure 1 seconde.
 *
 * Sortie : chaque lecture (avec la valeur lue) et chaque écriture, puis "Fin du traitement".
 *
 * Exemple de sortie attendue (l'ordre peut varier d'une exécution à l'autre) :
 * <pre>
 * Reader-1 lit : Valeur initiale
 * Reader-3 lit : Valeur initiale
 * Reader-2 lit : Valeur initiale
 * Writer-2 écrit : Nouvelle valeur 0
 * Writer-2 écrit : Nouvelle valeur 1
 * Writer-1 écrit : Nouvelle valeur 0
 * ...
 * Reader-1 lit : Nouvelle valeur 2
 * ...
 * Fin du traitement
 * </pre>
 *
 * À observer : les trois "lit" d'un même groupe s'affichent presque en même temps (lectures parallèles), alors que les "écrit" arrivent un par seconde (écriture exclusive). Aucune lecture n'a lieu pendant une écriture.
 *
 * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.lecteurredacteur.etape1_readwritelock.ReadWriteLockDemo}
 */
public class ReadWriteLockDemo {

    public static void main(String[] args) throws InterruptedException {
        SharedData sharedData = new SharedData();

        List<Thread> threads = List.of(
                new Reader(sharedData, "Reader-1"),
                new Reader(sharedData, "Reader-2"),
                new Reader(sharedData, "Reader-3"),
                new Writer(sharedData, "Writer-1"),
                new Writer(sharedData, "Writer-2"));

        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        System.out.println("Fin du traitement");
    }
}

package fr.vt.simulation.threadclassique.collections;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Démo ConcurrentHashMap : deux threads écrivent dans la même map sans synchronisation externe.
 *
 * But : montrer qu'une {@link ConcurrentHashMap} supporte les écritures et lectures concurrentes sans corruption ni exception.
 *
 * Entrée : aucune. Le thread 1 écrit 0 à 4 sous la clé "A", le thread 2 fait de même sous la clé "B".
 *
 * Sortie : le contenu de la map après chaque écriture, puis le contenu final.
 *
 * Exemple de sortie attendue (l'ordre peut varier d'une exécution à l'autre) :
 * <pre>
 * Thread 2 : {A=0, B=0}
 * Thread 1 : {A=0}
 * Thread 2 : {A=0, B=2}
 * Thread 1 : {A=1, B=2}
 * ...
 * Contenu final : {A=4, B=4}
 * </pre>
 *
 * À observer : les affichages intermédiaires s'entrelacent et peuvent sembler incohérents
 * (une ligne peut ne pas encore voir la clé de l'autre thread) : la lecture est "weakly consistent". Le contenu final est toujours {A=4, B=4}.
 *
 * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.collections.ConcurrentHashMapDemo}
 */
public class ConcurrentHashMapDemo {

    public static void main(String[] args) throws InterruptedException {
        Map<String, Integer> map = new ConcurrentHashMap<>();

        Thread t1 = new Thread(() -> writeValues(map, "A", "Thread 1"));
        Thread t2 = new Thread(() -> writeValues(map, "B", "Thread 2"));

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("Contenu final : " + map);
    }

    private static void writeValues(Map<String, Integer> map, String key, String threadName) {
        for (int i = 0; i < 5; i++) {
            map.put(key, i);
            // L'itération faite par toString() est "weakly consistent" : pas d'exception,
            // mais l'écriture de l'autre thread peut être visible ou non à cet instant
            System.out.println(threadName + " : " + map);
        }
    }
}

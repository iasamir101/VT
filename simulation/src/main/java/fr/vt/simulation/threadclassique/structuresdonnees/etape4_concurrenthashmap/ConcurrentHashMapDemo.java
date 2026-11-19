package fr.vt.simulation.threadclassique.structuresdonnees.etape4_concurrenthashmap;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Démo ConcurrentHashMap : HashMap et ConcurrentHashMap face aux mêmes écritures concurrentes.
 *
 * But : montrer qu'une {@link HashMap} perd des données quand deux threads l'écrivent en même temps,
 * alors qu'une {@link ConcurrentHashMap} n'en perd pas.
 *
 * Entrée : aucune. Deux threads insèrent chacun 100 000 clés différentes
 * (0 à 99 999 pour l'un, 100 000 à 199 999 pour l'autre).
 *
 * Sortie : le nombre de clés attendu et le nombre de clés obtenu pour chaque type de map.
 *
 * Exemple de sortie attendue (la valeur fausse change à chaque exécution) :
 * <pre>
 * Insertion de clés différentes (attendu : 200000 clés)
 *    HashMap           : 177913 clés
 *    ConcurrentHashMap : 200000 clés
 * </pre>
 *
 * À observer : la HashMap a presque toujours moins de 200 000 clés (elle peut aussi lever une exception) ;
 * la ConcurrentHashMap en a toujours 200 000.
 *
 * Pourquoi une version plus simple ne montre rien : si chaque thread réécrit toujours la même clé
 * ("A" pour l'un, "B" pour l'autre), la map ne contient que 2 entrées, ne se redimensionne jamais
 * et aucune mise à jour ne peut se perdre. Les deux maps donnent alors le même résultat.
 * Les System.out.println dans les boucles masquent aussi le problème : println est synchronisé
 * et ralentit tellement les threads qu'ils ne s'entrelacent presque plus.
 *
 * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.structuresdonnees.etape4_concurrenthashmap.ConcurrentHashMapDemo}
 */
public class ConcurrentHashMapDemo {

    // Nombre d'insertions faites par CHAQUE thread.
    // Il en faut beaucoup : avec quelques dizaines, un thread finit avant que l'autre démarre
    // et les deux ne se marchent jamais dessus.
    private static final int PER_THREAD = 100_000;

    // Deux threads x PER_THREAD insertions : ce qu'on doit obtenir si rien n'est perdu
    private static final int EXPECTED = 2 * PER_THREAD;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Insertion de clés différentes (attendu : " + EXPECTED + " clés)");
        System.out.println("   HashMap           : " + insertTo(new HashMap<>()) + " clés");
        System.out.println("   ConcurrentHashMap : " + insertTo(new ConcurrentHashMap<>()) + " clés");
    }

    /**
     * Deux threads insèrent chacun PER_THREAD clés distinctes dans la même map.
     *
     * Les threads n'écrivent jamais la même clé, et pourtant la HashMap en perd :
     * c'est sa structure interne (tableau de cases, redimensionnement) qui est partagée.
     * Dans une HashMap, deux put() simultanés peuvent :
     * - viser la même case du tableau interne : l'un écrase le chaînage créé par l'autre ;
     * - tomber pendant un redimensionnement (resize) : des entrées sont recopiées dans l'ancien tableau
     *   et disparaissent.
     * La ConcurrentHashMap verrouille chaque case séparément (et gère le resize à plusieurs threads),
     * donc aucune insertion n'est perdue.
     *
     * @return le nombre de clés réellement présentes à la fin
     */
    private static int insertTo(Map<Integer, Integer> map) throws InterruptedException {
        // t2 : la moitié basse des clés, de 0 à 99 999
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < PER_THREAD; i++) {
                map.put(i, i);
            }
        });

        // t1 : la moitié haute, de 100 000 à 199 999 (aucune clé en commun avec t2)
        Thread t1 = new Thread(() -> {
            for (int i = PER_THREAD; i < EXPECTED; i++) {
                map.put(i, i);
            }
        });

        // Les deux start() sont appelés avant tout join() : les threads tournent en parallèle
        t2.start();
        t1.start();

        // join() : main attend la fin de chaque thread avant de lire la taille,
        // et voit alors toutes leurs écritures (garantie du modèle mémoire Java).
        // Si une HashMap corrompue lève une exception dans un thread, elle s'affiche
        // sur la sortie d'erreur et join() rend quand même la main.
        t2.join();
        t1.join();

        return map.size();
    }
}

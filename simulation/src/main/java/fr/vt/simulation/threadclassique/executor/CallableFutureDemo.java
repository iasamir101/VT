package fr.vt.simulation.threadclassique.executor;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Démo Callable et Future : récupérer le résultat d'une tâche exécutée dans un pool.
 *
 * But : montrer la différence avec {@link Runnable} : un {@link Callable} renvoie une valeur, que l'on récupère via le {@link Future} renvoyé par {@code submit()}.
 *
 * Entrée : aucune. Une tâche qui attend 2 secondes puis renvoie 10 + 20.
 *
 * Sortie : le résultat du calcul.
 *
 * Exemple de sortie attendue (l'ordre peut varier d'une exécution à l'autre) :
 * <pre>
 * Résultat : 30
 * </pre>
 *
 * À observer : le résultat s'affiche après environ 2 secondes : {@code future.get()} bloque le thread principal tant que la tâche n'est pas finie.
 *
 * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.executor.CallableFutureDemo}
 */
public class CallableFutureDemo {

    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // Contrairement à Runnable, Callable renvoie une valeur et peut lever une exception
            Callable<Integer> task = () -> {
                Thread.sleep(2000); // calcul long simulé
                return 10 + 20;
            };

            // submit() rend la main tout de suite ; la tâche tourne dans un thread du pool
            Future<Integer> future = executor.submit(task);

            // get() bloque le thread principal jusqu'à ce que le résultat soit prêt (~2 s)
            System.out.println("Résultat : " + future.get());
        } finally {
            executor.shutdown();
        }
    }
}

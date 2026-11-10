package fr.vt.simulation.threadclassique.executor;

import java.util.concurrent.CompletableFuture;

/**
 * Démo CompletableFuture.supplyAsync : lancer un calcul asynchrone qui renvoie un résultat.
 *
 * But : montrer comment obtenir une valeur d'une tâche asynchrone avec {@code supplyAsync} puis {@code join()}.
 *
 * Entrée : aucune. Un calcul qui renvoie 10 + 20.
 *
 * Sortie : le résultat du calcul.
 *
 * Exemple de sortie attendue (l'ordre peut varier d'une exécution à l'autre) :
 * <pre>
 * Résultat : 30
 * </pre>
 *
 * À observer : {@code join()} attend comme {@code Future.get()}, mais sans exception vérifiée à déclarer.
 *
 * Voir aussi : {@link CompletableFutureRunAsyncDemo} (sans résultat).
 *
 * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.executor.CompletableFutureSupplyAsyncDemo}
 */
public class CompletableFutureSupplyAsyncDemo {

    public static void main(String[] args) {
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> 10 + 20);

        // join() attend le résultat comme get(), mais lève une exception non vérifiée
        // (CompletionException) : pas de throws ni de try/catch obligatoire
        Integer result = future.join();

        System.out.println("Résultat : " + result);
    }
}

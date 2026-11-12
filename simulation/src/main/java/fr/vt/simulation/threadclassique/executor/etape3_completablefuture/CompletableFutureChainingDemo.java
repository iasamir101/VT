package fr.vt.simulation.threadclassique.executor.etape3_completablefuture;

import java.util.concurrent.CompletableFuture;

/**
 * Démo CompletableFuture : enchaîner des étapes avec thenApply, thenAccept et thenRun.
 *
 * But : montrer qu'on peut décrire "quand c'est fini, fais ceci" sans jamais bloquer avec get() ou join()
 * entre les étapes.
 *
 * Entrée : aucune. Un calcul qui renvoie 10, puis trois étapes enchaînées.
 *
 * Sortie : une ligne par étape, avec le nom du thread qui l'exécute.
 *
 * Exemple de sortie attendue (le nom du thread peut varier) :
 * <pre>
 * Thread principal : continue sans attendre
 * supplyAsync  : 10 [ForkJoinPool.commonPool-worker-1]
 * thenApply    : 20 [ForkJoinPool.commonPool-worker-1]
 * thenAccept   : 20 [ForkJoinPool.commonPool-worker-1]
 * thenRun      : chaîne terminée
 * </pre>
 *
 * À observer : "Thread principal" s'affiche en premier, car la chaîne s'exécute en arrière-plan.
 * Chaque étape démarre quand la précédente est finie.
 *
 * Méthodes de chaînage (toutes renvoient un nouveau CompletableFuture) :
 * <pre>
 * thenApply(x -> y)   reçoit le résultat, renvoie un nouveau résultat   (comme map)
 * thenAccept(x -> )   reçoit le résultat, ne renvoie rien               (consommer)
 * thenRun(() -> )     ne reçoit rien, ne renvoie rien                   (réagir à la fin)
 * </pre>
 * Si une étape échoue, les suivantes sont sautées et l'erreur ressort au join()
 * (CompletionException) ; elle peut être traitée avec exceptionally() ou handle().
 *
 * Voir aussi : {@link CompletableFutureSupplyAsyncDemo} (le point de départ).
 *
 * Lancer : {@code mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.executor.etape3_completablefuture.CompletableFutureChainingDemo}
 */
public class CompletableFutureChainingDemo {

    public static void main(String[] args) throws InterruptedException {

        // La chaîne décrit les étapes ; chacune démarre quand la précédente est finie :
        // - supplyAsync : produit une valeur (10) dans le pool commun
        // - thenApply   : reçoit le résultat et en renvoie un NOUVEAU (transformer)
        // - thenAccept  : reçoit le résultat et ne renvoie rien (consommer, fin de chaîne)
        // - thenRun     : ne reçoit rien et ne renvoie rien (juste "quand c'est fini")
        CompletableFuture<Void> future = CompletableFuture
                .supplyAsync(() -> produceValue())
                .thenApply(x -> doubleValue(x))
                .thenAccept(x -> print("thenAccept  ", x))
                .thenRun(() -> System.out.println("thenRun      : chaîne terminée"));

        // La chaîne tourne en arrière-plan : main n'est pas bloqué
        System.out.println("Thread principal : continue sans attendre");
        // Seul point d'attente : sans lui la JVM pourrait s'arrêter avant la fin (threads du pool commun daemon)
        future.join();
    }

    /**
     * Étape 1 (supplyAsync) : produit la valeur de départ après un calcul long simulé (1 s).
     * Elle s'exécute dans un thread du pool commun, pas dans main.
     *
     * @return la valeur produite (10)
     */
    private static int produceValue() {
        sleep(1000);
        print("supplyAsync ", 10);
        return 10;
    }

    /**
     * Étape 2 (thenApply) : transforme le résultat de l'étape précédente.
     * Elle démarre seulement quand produceValue() est terminée, et reçoit sa valeur en paramètre.
     *
     * @param x le résultat de l'étape précédente
     * @return le double de x, passé à l'étape suivante
     */
    private static int doubleValue(Integer x) {
        print("thenApply   ", x * 2);
        return x * 2;
    }

    /** Affiche une étape avec le nom du thread qui l'exécute. */
    private static void print(String step, int value) {
        System.out.println(step + ": " + value + " [" + Thread.currentThread().getName() + "]");
    }

    /** Simule un calcul long. */
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restaure le statut d'interruption avant de sortir
            throw new RuntimeException(e);
        }
    }
}

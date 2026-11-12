# Fiches de révision : pools et futures

Package racine : `fr.vt.simulation.threadclassique` (la colonne « Package » ci-dessous est relative à cette racine).

Cinq démos, expliquées une par une : notion, déroulement, avantages, limites, astuces, bonnes pratiques et points à retenir.

| Démo | Package | Notion |
|---|---|---|
| [`FixedThreadPoolDemo`](#1-fixedthreadpooldemo--le-pool-de-threads) | `executor.etape1_pool` | `ExecutorService`, pool fixe, `shutdown()` |
| [`CallableFutureDemo`](#2-callablefuturedemo--récupérer-un-résultat) | `executor.etape2_callablefuture` | `Callable`, `Future`, `get()` |
| [`CompletableFutureRunAsyncDemo`](#3-completablefuturerunasyncdemo--tâche-asynchrone-sans-résultat) | `executor.etape3_completablefuture` | `runAsync`, pool commun |
| [`CompletableFutureSupplyAsyncDemo`](#4-completablefuturesupplyasyncdemo--tâche-asynchrone-avec-résultat) | `executor.etape3_completablefuture` | `supplyAsync`, `join()` |
| [`CompletableFutureChainingDemo`](#5-completablefuturechainingdemo--enchaîner-les-étapes) | `executor.etape3_completablefuture` | `thenApply`, `thenAccept`, `thenRun` |

Commande pour lancer une démo (depuis `simulation/`) :

```bash
mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.<package>.<Demo>
```

Les cinq démos forment une progression, reflétée par les packages `etape1_pool`, `etape2_callablefuture` et `etape3_completablefuture` : **pool de threads**, puis **résultat d'une tâche** (`Future`), puis **résultat sans bloquer** (`CompletableFuture`), puis **enchaînement des étapes**.

---

## 1. `FixedThreadPoolDemo` : le pool de threads

### Notion
Créer un thread par tâche coûte cher (mémoire, création, ordonnancement). Un **pool** crée quelques threads une fois et les **réutilise** pour exécuter de nombreuses tâches. `Executors.newFixedThreadPool(3)` donne un pool de **3 threads**, ni plus ni moins.

### Déroulement
1. `newFixedThreadPool(3)` crée un pool. Les threads sont créés à la demande, jusqu'à 3, et nommés `pool-N-thread-M`.
2. `submit(tâche)` dépose la tâche dans une **file d'attente** et rend la main tout de suite.
3. Un thread libre prend la tâche et l'exécute. Avec plus de 3 tâches, les suivantes attendent dans la file.
4. `shutdown()` ferme le pool : plus de nouvelles tâches, mais les tâches déjà soumises vont à leur terme. Les threads s'arrêtent ensuite.

### Pourquoi `shutdown()` est obligatoire
Les threads du pool sont des threads **non daemon** : ils restent en attente de travail après la dernière tâche. La JVM ne s'arrête que s'il ne reste que des threads daemon. Sans `shutdown()`, le programme affiche ses lignes puis **reste bloqué** (Ctrl+C pour sortir). Le `try/finally` garantit l'appel même en cas d'exception.

### Avantages
- Réutilisation des threads : moins de création et de destruction.
- Plafond de concurrence : jamais plus de N tâches en parallèle, donc la machine ne sature pas.
- Séparation entre *quoi faire* (la tâche) et *comment l'exécuter* (le pool).

### Limites
- **La file d'attente est non bornée** : si les tâches arrivent plus vite qu'elles ne sont traitées, la file grossit jusqu'à `OutOfMemoryError`.
- Une tâche longue ou bloquante occupe un des N threads. Avec N tâches bloquées, plus rien ne passe (famine).
- L'ordre d'affichage entre tâches n'est pas garanti.
- Un `Runnable` soumis avec `submit()` **avale ses exceptions** : sans appel à `get()` sur le `Future`, l'erreur disparaît en silence.

### Astuces
- Taille du pool : tâches **CPU** : environ le nombre de cœurs (`Runtime.getRuntime().availableProcessors()`). Tâches **bloquantes** (réseau, disque, BDD) : plus de threads, à mesurer.
- Pour attendre la fin des tâches : `shutdown()` puis `awaitTermination(délai, unité)`.
- Depuis Java 19, `ExecutorService` est `AutoCloseable` : `try (var executor = ...) { ... }` appelle `close()` (qui fait `shutdown` + attente).
- `execute(Runnable)` laisse l'exception remonter au thread, `submit()` la range dans le `Future`.

### Bonnes pratiques
- Toujours arrêter le pool (`finally` ou try-with-resources).
- En production, préférer un `ThreadPoolExecutor` avec **file bornée** et politique de rejet explicite à `newFixedThreadPool`.
- Donner des noms parlants aux threads via une `ThreadFactory` : les logs et thread dumps deviennent lisibles.
- Un pool par type de travail (CPU, I/O) pour qu'un type ne bloque pas l'autre.

### À retenir
> Pool = N threads réutilisés + une file d'attente. **Pas de `shutdown()` = programme qui ne se termine pas.** `newFixedThreadPool` a une file illimitée.

---

## 2. `CallableFutureDemo` : récupérer un résultat

### Notion
`Runnable.run()` ne renvoie rien et ne peut pas lever d'exception vérifiée. **`Callable<V>.call()`** renvoie une valeur de type `V` et peut lever une exception. `submit(callable)` renvoie un **`Future<V>`** : une « promesse » de résultat, à récupérer plus tard.

### Déroulement
1. Trois `Callable<Integer>` renvoient 10, 20 et 30 : le premier répond tout de suite, les deux autres dorment 2 s (calcul long simulé).
2. Les trois `submit()` rendent la main immédiatement. Les tâches démarrent **en parallèle**, une par thread du pool de 3.
3. `future1.get()` bloque `main` jusqu'à la fin de la tâche 1. Les deux autres ont démarré en même temps, donc `get()` suivants rendent la valeur sans attendre.
4. Total : **environ 2 s**, pas 6 s. Avec un pool d'**1 seul thread**, on mesurerait environ 6 s, car les tâches passeraient l'une après l'autre.

**Deuxième partie de la démo : `invokeAll`.** La méthode `collection()` fabrique une liste de trois `Callable` (1 s, 2 s, 3 s) et `executor.invokeAll(collection())` les soumet **en un seul appel** :
1. `invokeAll` **bloque** jusqu'à ce que *toutes* les tâches soient finies (environ 3 s mesurées : la plus longue, pas la somme de 6 s).
2. Il renvoie une `List<Future<Integer>>` dans le **même ordre** que la collection fournie.
3. Les futures sont déjà terminés : `isDone()` vaut `true` et `get()` ne bloque plus.

| | `submit` (un par un) | `invokeAll` (en lot) |
|---|---|---|
| Rend la main | tout de suite | quand toutes les tâches sont finies |
| `get()` ensuite | peut bloquer | ne bloque plus |
| Pratique pour | tâches indépendantes, résultats lus à des moments différents | « lance tout et donne-moi tout » |

Variante `invokeAny` : renvoie le résultat de la **première** tâche réussie et annule les autres. `invokeAll(tâches, délai, unité)` annule les tâches non terminées au bout du délai.

### Avantages
- Les tâches s'exécutent en parallèle : le temps total est celui de la plus longue, pas la somme.
- Les exceptions de la tâche sont transmises à l'appelant (voir ci-dessous).
- API simple : `submit` puis `get`.

### Limites
- **`get()` est bloquant** : `main` attend. Si on appelle `get()` juste après chaque `submit()`, on perd tout le parallélisme.
- Pas de chaînage (« quand c'est fini, fais ça ») ni de callback : il faut bloquer pour lire le résultat.
- `get()` lève des exceptions vérifiées (`InterruptedException`, `ExecutionException`) à gérer ou à déclarer.
- Pas de combinaison simple de plusieurs futures : c'est ce que résout `CompletableFuture`.

### Astuces
- **Soumettre toutes les tâches d'abord, puis appeler `get()`** : c'est ce que fait la démo, et c'est ce qui donne le gain de temps.
- `get(délai, unité)` lève `TimeoutException` au lieu d'attendre indéfiniment.
- Si la tâche a levé une exception, `get()` renvoie une `ExecutionException` dont `getCause()` contient l'erreur d'origine.
- `future.cancel(true)` tente d'annuler (interruption du thread) ; `isDone()` teste sans bloquer.
- `executor.invokeAll(liste)` lance plusieurs `Callable` et attend qu'ils soient tous finis ; `invokeAny` renvoie le premier résultat obtenu.

### Bonnes pratiques
- Toujours prévoir un timeout sur `get()` pour les appels externes.
- Restaurer l'interruption si on attrape `InterruptedException` : `Thread.currentThread().interrupt()`.
- Ne jamais appeler `get()` depuis une tâche du **même** pool saturé : risque de blocage mutuel (deadlock par famine).

### À retenir
> `Callable` = tâche avec résultat. `Future.get()` **bloque**. Lancer toutes les tâches avant de lire les résultats pour garder le parallélisme.

---

## 3. `CompletableFutureRunAsyncDemo` : tâche asynchrone sans résultat

### Notion
`CompletableFuture` est un `Future` amélioré : on peut y **chaîner des actions**, les combiner et gérer les erreurs, sans bloquer. `runAsync(Runnable)` lance une tâche **sans valeur de retour** (`CompletableFuture<Void>`).

### Déroulement
1. `runAsync(...)` démarre la tâche dans un autre thread et rend la main tout de suite.
2. `main` continue et affiche « Thread principal ». Cette ligne peut apparaître **avant ou après** celles de la tâche : aucun ordre garanti.
3. La tâche affiche `ForkJoinPool.commonPool-worker-1` : sans executor fourni, elle tourne dans le **pool commun** (`ForkJoinPool.commonPool()`).
4. `join()` fait attendre `main` jusqu'à la fin de la tâche.

### Pourquoi `join()` à la fin
Les threads du pool commun sont des threads **daemon** : la JVM ne les attend pas. Si `main` se termine avant la fin de la tâche, le programme s'arrête et la tâche peut ne **jamais s'exécuter**. C'est l'inverse du pool fixe de la démo 1, dont les threads non daemon empêchent l'arrêt.

### Avantages
- Aucun pool à créer ni à arrêter pour un usage simple.
- Base pour construire des chaînes asynchrones (`thenRun`, `thenAccept`, etc.).

### Limites
- Pas de résultat (`Void`) : adapté aux effets de bord (log, envoi, écriture).
- Le pool commun est **partagé** par toute l'application (et par les streams parallèles) et sa taille est petite (environ nombre de cœurs moins 1).
- Une exception dans la tâche n'apparaît nulle part tant qu'on n'appelle pas `join()`, `get()` ou `exceptionally`.

### Astuces
- Fournir son propre pool : `runAsync(tâche, executor)`.
- Enchaîner : `.thenRun(() -> ...)` s'exécute quand la tâche est finie.
- Les quatre `println` identiques de la démo s'exécutent **dans l'ordre, dans le même thread** : seule la tâche entière est asynchrone par rapport à `main`.

### Bonnes pratiques
- Ne **pas** mettre de tâche bloquante (sleep, I/O) dans le pool commun : elle prive les autres du pool. Utiliser un executor dédié.
- Toujours traiter l'erreur (`exceptionally`, `handle`) ou attendre la fin (`join`) : sinon l'échec est silencieux.
- Attendre explicitement la fin dans un `main` ou un test.

### À retenir
> `runAsync` = « lance et oublie » dans le pool commun. Threads **daemon** : sans `join()`, la JVM peut quitter avant la fin.

---

## 4. `CompletableFutureSupplyAsyncDemo` : tâche asynchrone avec résultat

### Notion
`supplyAsync(Supplier<T>)` est l'équivalent de `runAsync` **avec un résultat** : il renvoie un `CompletableFuture<T>`. C'est l'équivalent moderne de `submit(Callable)`.

### Déroulement
1. `supplyAsync(...)` démarre un calcul (2 s de `sleep`, puis `10 + 20`) dans le pool commun.
2. `main` affiche « Thread principal 01 » **immédiatement** : il n'est pas bloqué.
3. `future.join()` attend le résultat (environ 2 s) puis renvoie `30`.
4. « Résultat : 30 » puis « Thread principal 02 » s'affichent.

### `join()` ou `get()`
| | `get()` | `join()` |
|---|---|---|
| Attend le résultat | oui | oui |
| Exceptions vérifiées | `InterruptedException`, `ExecutionException` | aucune |
| Erreur de la tâche | `ExecutionException` | `CompletionException` (non vérifiée) |

`join()` est pratique dans les lambdas et les chaînes, où les exceptions vérifiées gênent. Dans les deux cas l'exception d'origine est dans `getCause()`.

### Avantages
- Le thread principal peut **travailler pendant que le calcul tourne**, avant de lire le résultat.
- Chaînage sans blocage : `thenApply` (transformer), `thenAccept` (consommer), `thenCompose` (enchaîner un autre futur), `thenCombine` (fusionner deux résultats), `allOf` / `anyOf` (attendre plusieurs).
- Gestion d'erreur intégrée : `exceptionally`, `handle`, `whenComplete`.
- Délai maximal : `orTimeout(...)`, `completeOnTimeout(...)` (Java 9+).

### Limites
- Même problème du pool commun que `runAsync` : petit, partagé, daemon.
- Dans la démo, `Thread.sleep` bloque un thread du pool commun, ce qui n'est acceptable que pour une démo.
- Dans le `catch` de la démo, `throw new RuntimeException(e)` perd le statut d'interruption : à éviter en vrai code.
- Une chaîne trop longue devient difficile à lire et à déboguer.

### Astuces
- Un exemple de chaîne : `supplyAsync(() -> 10).thenApply(x -> x + 20).thenAccept(System.out::println)`. Aucun `join()` n'est nécessaire dans la chaîne elle-même, seulement pour attendre la fin dans `main`.
- `thenApply` s'exécute dans le thread qui a fini la tâche (ou dans l'appelant si elle est déjà finie). `thenApplyAsync` relance dans un pool.
- Plusieurs appels en parallèle : lancer tous les `supplyAsync`, puis `CompletableFuture.allOf(...).join()`.

### Bonnes pratiques
- Passer un **executor dédié** pour tout ce qui bloque (réseau, BDD).
- Prévoir un timeout et une gestion d'erreur : un futur jamais complété bloque `join()` indéfiniment.
- Ne pas appeler `join()` dans une chaîne asynchrone si on peut composer avec `thenCompose`.

### À retenir
> `supplyAsync` = résultat asynchrone **sans bloquer** tant qu'on ne lit pas. `join()` = `get()` sans exceptions vérifiées. Les vrais avantages sont le **chaînage** et la **gestion d'erreur**.

---

## 5. `CompletableFutureChainingDemo` : enchaîner les étapes

### Notion
Avec `Future`, il faut bloquer (`get()`) pour utiliser un résultat. Avec `CompletableFuture`, on décrit **ce qu'il faut faire quand l'étape précédente est finie**, sans jamais bloquer entre les étapes. Chaque méthode `thenXxx` renvoie un nouveau `CompletableFuture`, ce qui permet d'écrire une chaîne.

| Méthode | Reçoit | Renvoie | Rôle |
|---|---|---|---|
| `thenApply(x -> y)` | le résultat | un nouveau résultat | transformer (comme `map`) |
| `thenAccept(x -> ...)` | le résultat | rien | consommer (afficher, enregistrer) |
| `thenRun(() -> ...)` | rien | rien | réagir à la fin |

### Déroulement
1. `supplyAsync(produceValue)` démarre dans le pool commun : 1 s de calcul simulé, puis la valeur **10**.
2. `main` n'attend pas : « Thread principal : continue sans attendre » s'affiche **en premier**.
3. Quand la valeur est prête, `thenApply(doubleValue)` la transforme en **20**.
4. `thenAccept` affiche 20 (fin de la chaîne de valeurs : plus rien à transmettre).
5. `thenRun` affiche « chaîne terminée ».
6. `join()` fait attendre `main` sur le dernier maillon : c'est le **seul** point bloquant.

### Avantages
- Aucun thread bloqué entre les étapes : on décrit un enchaînement, pas une attente.
- Code lisible, dans l'ordre logique : produire, transformer, consommer.
- Une seule gestion d'erreur possible pour toute la chaîne.

### Limites
- **Le thread d'exécution d'une étape n'est pas garanti.** Dans la démo, les étapes tournent toutes dans `ForkJoinPool.commonPool-worker-N` (le thread qui a fini l'étape précédente). Si l'étape précédente est déjà finie, `thenApply` s'exécute dans le thread **appelant**.
- Une étape longue ou bloquante occupe ce thread, souvent du pool commun.
- Si une étape lève une exception, **toutes les suivantes sont sautées** et l'erreur n'apparaît qu'au `join()` (`CompletionException`).
- `thenAccept` et `thenRun` renvoient `CompletableFuture<Void>` : la valeur est perdue après eux.
- Une longue chaîne devient difficile à déboguer (les piles d'appels sont fragmentées).

### Astuces
- Les variantes `thenApplyAsync`, `thenAcceptAsync`, `thenRunAsync` relancent l'étape dans un pool (le pool commun, ou un executor passé en 2e paramètre) au lieu de réutiliser le thread courant.
- Pour traiter une erreur dans la chaîne : `exceptionally(e -> valeurDeSecours)` ou `handle((résultat, erreur) -> ...)`.
- `whenComplete((résultat, erreur) -> ...)` observe sans modifier la valeur (logs).
- Afficher `Thread.currentThread().getName()` dans chaque étape : c'est le moyen le plus simple de voir qui exécute quoi.
- Si l'étape suivante renvoie elle-même un `CompletableFuture`, utiliser **`thenCompose`** (équivalent de `flatMap`) plutôt que `thenApply`, qui donnerait un futur imbriqué.
- Pour fusionner deux tâches indépendantes : `thenCombine`. Pour en attendre plusieurs : `allOf` / `anyOf`.

### Bonnes pratiques
- Terminer la chaîne par une gestion d'erreur : sans elle, un échec reste silencieux tant que personne n'appelle `join()`.
- Garder chaque étape **courte et sans effet de bord caché** ; passer un executor dédié pour ce qui bloque.
- Un seul `join()` à la toute fin (dans `main` ou un test), pas entre les étapes : sinon on retrouve le blocage de `Future.get()`.
- Nommer les étapes dans des méthodes (`produceValue`, `doubleValue`) plutôt que des lambdas longues : la chaîne se lit comme une phrase.

### À retenir
> `thenApply` = **transformer**, `thenAccept` = **consommer**, `thenRun` = **réagir**. Chaque étape démarre quand la précédente est finie, sans bloquer ; un seul `join()` à la fin. Une erreur saute toutes les étapes suivantes.

---

## Comparatif rapide : choisir le bon outil

| Besoin | Outil |
|---|---|
| Exécuter beaucoup de tâches avec un nombre de threads borné | `ExecutorService` (pool) |
| Lancer une tâche et lire son résultat plus tard | `Callable` + `Future` |
| Lancer une tâche sans résultat, sans bloquer | `CompletableFuture.runAsync` |
| Lancer plusieurs `Callable` d'un coup et attendre qu'ils soient tous finis | `invokeAll` |
| Lancer un calcul, enchaîner, combiner, gérer les erreurs | `CompletableFuture.supplyAsync` + `thenXxx` |
| Transformer / consommer / réagir à un résultat sans bloquer | `thenApply` / `thenAccept` / `thenRun` |

| | `Future` | `CompletableFuture` |
|---|---|---|
| Lire le résultat | `get()` (bloquant) | `join()` / `get()` ou chaînage sans bloquer |
| Enchaîner des actions | non | oui (`thenApply`, `thenCompose`) |
| Combiner plusieurs tâches | à la main | `thenCombine`, `allOf`, `anyOf` |
| Gérer les erreurs | `ExecutionException` au `get()` | `exceptionally`, `handle` |
| Timeout | `get(délai)` | `orTimeout`, `completeOnTimeout` |

## Questions d'entretien fréquentes

1. **Pourquoi un pool plutôt qu'un `new Thread` par tâche ?** Coût de création, plafond de concurrence, réutilisation.
2. **Que se passe-t-il sans `shutdown()` ?** Les threads du pool sont non daemon : la JVM ne s'arrête pas.
3. **Différence `Runnable` / `Callable` ?** Résultat et exceptions vérifiées.
4. **`execute` ou `submit` ?** `submit` renvoie un `Future` et capture l'exception ; `execute` non.
5. **Quel est le défaut de `get()` ?** Il bloque ; d'où `CompletableFuture`.
6. **Quel pool utilise `supplyAsync` par défaut, et quel est le risque ?** Le pool commun (daemon, petit, partagé) : ne pas y mettre de tâches bloquantes.
7. **Différence entre `submit` et `invokeAll` ?** `submit` rend la main tout de suite pour une tâche ; `invokeAll` soumet une collection et bloque jusqu'à ce que toutes soient finies.
8. **Différence `thenApply` / `thenAccept` / `thenRun` ?** Transformer le résultat / le consommer sans rien renvoyer / réagir à la fin sans le recevoir.
9. **`thenApply` ou `thenCompose` ?** `thenApply` quand la fonction renvoie une valeur ; `thenCompose` quand elle renvoie un `CompletableFuture` (évite l'imbrication).
10. **Que se passe-t-il si une étape de la chaîne échoue ?** Les étapes suivantes sont sautées et l'erreur ressort au `join()` en `CompletionException`, sauf si `exceptionally` ou `handle` la traite.

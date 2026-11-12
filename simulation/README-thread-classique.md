# Multithreading classique : vue d'ensemble

Package racine : `fr.vt.simulation.threadclassique`

Ce document résume les cinq fiches de révision du projet et donne un **ordre d'apprentissage global**. Chaque section dit : le problème, les outils, ce qu'il faut retenir, et renvoie à la fiche détaillée.

| Thème | Package | Fiche détaillée |
|---|---|---|
| Synchronisation (race condition, `synchronized`, synchroniseurs) | `synchroniseurs.*` (de `etape0_race` à `etape5_exchanger`) | [`README-synchroniseurs.md`](README-synchroniseurs.md) |
| Structures de données thread-safe | `structuresdonnees.*` | [`README-structures-de-donnees.md`](README-structures-de-donnees.md) |
| Pools de threads et futures | `executor.*` | [`README-pools-et-futures.md`](README-pools-et-futures.md) |
| Producteur / consommateur | `producteurconsommateur.*` | [`README-producteur-consommateur.md`](README-producteur-consommateur.md) |
| Lecteurs / rédacteurs | `lecteurredacteur.*` | [`README-lecteur-redacteur.md`](README-lecteur-redacteur.md) |

Lancer une démo (depuis `simulation/`) :

```bash
mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.<package>.<Demo>
```

Convention : les sous-packages d'un parcours s'appellent `etapeN_nom`, numérotés dans l'ordre d'apprentissage conseillé.

---

## Parcours conseillé

| Ordre | Partie | Pourquoi à cette place |
|---|---|---|
| 1 | **Synchronisation** : étape 0 (race), puis étapes 1 à 5 | On commence par le **problème** (la race condition), puis les outils qui coordonnent les threads. |
| 2 | **Structures de données** | Suite directe de la race condition : `AtomicInteger` la corrige sans verrou, puis le CAS sur un objet, puis `ConcurrentHashMap`. |
| 3 | **Pools et futures** | Après avoir compris la coordination, on cesse de créer des threads à la main : on les confie à un pool. |
| 4 | **Producteur / consommateur** | Premier schéma classique, d'abord à la main (`wait/notify`), puis avec une `BlockingQueue`. |
| 5 | **Lecteurs / rédacteurs** | Schéma où les lectures dominent : verrous de lecture et d'écriture, puis lecture optimiste. |

Pas encore couverts par une démo : le **deadlock** et tous les sujets **virtual threads**.

---

## 1. Synchronisation

Fiche détaillée : [`README-synchroniseurs.md`](README-synchroniseurs.md)

**Problème.** Plusieurs threads modifient la même donnée sans coordination : le résultat dépend de l'ordre dans lequel ils s'entrelacent.

| Étape | Package | Démo | Idée |
|---|---|---|---|
| **0** | `etape0_race` | `RaceConditionDemo` | **Le problème** : `counter++` = lire, ajouter, écrire (trois étapes). Deux threads perdent des incréments, de façon imprévisible. |
| 1 | `etape1_semaphore` | `SemaphoreParkingDemo` | Compteur de **permis** : limite à N le nombre de threads dans une zone. |
| 2 | `etape2_synchronized` | `SynchronizedCounterDemo` | Exclusion mutuelle : **un seul thread à la fois** + visibilité. Corrige la démo de l'étape 0. |
| 3 | `etape3_countdownlatch` | `CountDownLatchDemo` | « Attends que N événements soient faits » : sens unique, **usage unique**. |
| 4 | `etape4_cyclicbarrier` | `CyclicBarrierDemo` | « Attendons-nous tous, puis repartons » : **réutilisable**, avec une action de barrière. |
| 5 | `etape5_exchanger` | `ExchangerDemo` | Deux threads **s'échangent** une donnée à un point de rendez-vous. |

**À retenir**
- `counter++` n'est **pas atomique** : sans protection, des mises à jour sont perdues, et c'est probabiliste (un test qui passe ne prouve rien).
- `synchronized` : sûr et simple, mais bloquant, sans timeout. Verrou court, sur un objet **privé**.
- `Semaphore.release()` toujours dans un **`finally`** ; il n'a pas de propriétaire.
- `CountDownLatch` : un seul usage ; `countDown()` dans un `finally`. `CyclicBarrier` : réutilisable, mais un thread qui échoue **casse** la barrière.

---

## 2. Structures de données thread-safe

Fiche détaillée : [`README-structures-de-donnees.md`](README-structures-de-donnees.md)

**Problème.** Modifier une donnée partagée **sans perdre d'écriture**, avec ou sans verrou.

| Étape | Package | Démo | Idée |
|---|---|---|---|
| 1 | `etape1_atomicinteger` | `AtomicIntegerDemo` | Compteur **atomique** sans verrou, basé sur le **CAS** (Compare-And-Swap). |
| 2 | `etape2_atomicreference` | `AtomicReferenceDemo` | CAS sur un **objet** : `compareAndSet(attendu, nouveau)` ne réussit que pour un seul thread ; schéma lire, calculer, `compareAndSet`, recommencer en cas d'échec. |
| 3 | `etape3_concurrenthashmap` | `ConcurrentHashMapDemo` | Une `HashMap` perd des clés même quand elles sont distinctes ; `ConcurrentHashMap` n'en perd aucune. |

**À retenir**
- CAS : « écrire seulement si la valeur est toujours celle que j'ai lue, sinon recommencer ». Aucun thread n'est bloqué.
- `AtomicReference` compare par **référence** (`==`) ; attention au problème **ABA**.
- Atomique = **une seule variable**. Plusieurs variables liées demandent un verrou.
- `ConcurrentHashMap` protège **chaque appel**, pas une séquence : jamais `get` puis `put` ; utiliser `merge`, `compute`, `putIfAbsent`.

---

## 3. Pools de threads et futures

Fiche détaillée : [`README-pools-et-futures.md`](README-pools-et-futures.md)

**Problème.** Créer un thread par tâche coûte cher et ne plafonne pas la concurrence ; et il faut récupérer le résultat d'une tâche asynchrone.

| Étape | Package | Démo | Idée |
|---|---|---|---|
| 1 | `etape1_pool` | `FixedThreadPoolDemo` | Un **pool** de N threads réutilisés ; `shutdown()` obligatoire. |
| 2 | `etape2_callablefuture` | `CallableFutureDemo` | `Callable` renvoie un résultat ; `Future.get()` **bloque** ; `invokeAll` lance un lot. |
| 3 | `etape3_completablefuture` | `CompletableFutureRunAsyncDemo`, `…SupplyAsyncDemo`, `…ChainingDemo` | `runAsync` / `supplyAsync` dans le pool commun ; **chaînage** avec `thenApply`, `thenAccept`, `thenRun`. |

**À retenir**
- Pool = N threads + file d'attente. **Sans `shutdown()`, le programme ne se termine pas** (threads non daemon). `newFixedThreadPool` a une file **non bornée**.
- Soumettre **toutes** les tâches avant d'appeler `get()` : sinon on perd le parallélisme.
- Le pool commun de `CompletableFuture` a des threads **daemon**, est petit et partagé : pas de tâche bloquante dedans, et un `join()` en fin de `main`.
- `thenApply` = transformer, `thenAccept` = consommer, `thenRun` = réagir. Une étape qui échoue saute les suivantes ; l'erreur ressort au `join()`.

---

## 4. Producteur / consommateur

Fiche détaillée : [`README-producteur-consommateur.md`](README-producteur-consommateur.md)

**Problème.** Un producteur remplit un tampon, un consommateur le vide, à des vitesses différentes : chacun doit attendre l'autre sans rien perdre.

| Étape | Package | Démo | Idée |
|---|---|---|---|
| 1 | `etape1_waitnotify` | `WaitNotifyProducerConsumerDemo` | Tampon borné écrit à la main : `synchronized`, `wait()`, `notifyAll()`. |
| 2 | `etape2_blockingqueue` | `BlockingQueueProducerConsumerDemo` | `BlockingQueue` : `put()` bloque si pleine, `take()` si vide ; fin avec une *poison pill*. |

**À retenir**
- `wait()` **relâche le verrou** ; toujours dans un **`while`** (réveils intempestifs, condition redevenue fausse) ; `notifyAll()` plutôt que `notify()`.
- File **bornée** = contre-pression automatique.
- Une poison pill **par consommateur**. En vrai code, préférer la `BlockingQueue` au `wait/notify` écrit à la main.

---

## 5. Lecteurs / rédacteurs

Fiche détaillée : [`README-lecteur-redacteur.md`](README-lecteur-redacteur.md)

**Problème.** Une donnée est lue souvent et écrite rarement : autoriser plusieurs lecteurs en parallèle, mais un seul rédacteur, et jamais de lecture pendant une écriture.

| Étape | Package | Démo | Idée |
|---|---|---|---|
| 1 | `etape1_readwritelock` | `ReadWriteLockDemo` | `ReentrantReadWriteLock` : plusieurs lecteurs **ou** un seul rédacteur. |
| 2 | `etape2_stampedlock` | `StampedLockDemo` | `StampedLock` : **lecture optimiste** (lire sans verrou, `validate()`, sinon relire sous verrou). |

**À retenir**
- Pas de montée de verrou lecture vers écriture (le thread se bloque lui-même).
- `unlock()` dans un **`finally`** : un verrou explicite n'est pas libéré automatiquement.
- `StampedLock` : **non réentrant**, sans `Condition`, plus délicat ; rentable seulement si les écritures sont rares et si le gain est **mesuré**.

---

## Quel outil pour quel besoin

| Besoin | Outil |
|---|---|
| Un seul thread à la fois dans une section | `synchronized` |
| Limiter à N threads l'accès à une ressource | `Semaphore` |
| Attendre la fin de N tâches (une fois) | `CountDownLatch` |
| Faire avancer N threads par étapes, ensemble | `CyclicBarrier` |
| Échanger une donnée entre deux threads | `Exchanger` |
| Compteur ou indicateur partagé | `AtomicInteger` / `AtomicLong` / `AtomicBoolean` (`LongAdder` si très disputé) |
| Remplacer atomiquement un objet | `AtomicReference` (objet immuable) |
| Map partagée | `ConcurrentHashMap` (`merge` / `compute` / `putIfAbsent`) |
| Exécuter beaucoup de tâches avec N threads | `ExecutorService` (pool) |
| Récupérer un résultat plus tard | `Callable` + `Future` |
| Enchaîner / combiner des résultats asynchrones | `CompletableFuture` |
| Passer des éléments d'un thread à un autre | `BlockingQueue` |
| Lectures nombreuses, écritures rares | `ReentrantReadWriteLock`, puis `StampedLock` si mesuré |

## Réflexes valables partout

- **Libérer dans un `finally`** : `unlock()`, `release()`, `countDown()`, `shutdown()`.
- **Ne jamais ignorer `InterruptedException`** : restaurer le statut avec `Thread.currentThread().interrupt()`.
- **Toujours un timeout** quand on attend un système externe (`get(délai)`, `await(délai)`, `tryLock(délai)`, `poll(délai)`).
- **Garder les sections critiques courtes** : pas d'I/O ni de `sleep` en vrai code.
- **Ne pas partager** quand on peut : données locales au thread, objets immuables.
- **Un test qui passe ne prouve rien** : une race condition est probabiliste ; raisonner sur le code.
- **Les threads non daemon empêchent la JVM de s'arrêter** (pool fixe) ; les threads daemon (pool commun) ne sont pas attendus.

## Questions d'entretien transversales

1. **Qu'est-ce qu'une race condition ?** Plusieurs threads modifient une donnée partagée sans coordination ; le résultat dépend de l'entrelacement. Exemple : `counter++`.
2. **`synchronized`, atomique ou verrou explicite ?** Une variable simple : atomique. Plusieurs variables liées : `synchronized` ou verrou. Besoin de timeout ou d'équité : verrou explicite.
3. **Qu'est-ce que le CAS ?** Instruction atomique : écrire seulement si la valeur est celle attendue, sinon réessayer.
4. **Pourquoi un pool plutôt qu'un thread par tâche ?** Coût de création, plafond de concurrence, réutilisation.
5. **Que se passe-t-il sans `shutdown()` ?** La JVM ne s'arrête pas (threads du pool non daemon).
6. **`Future` ou `CompletableFuture` ?** `Future.get()` bloque ; `CompletableFuture` permet de chaîner, combiner et gérer les erreurs sans bloquer.
7. **`CountDownLatch` ou `CyclicBarrier` ?** Latch : usage unique, des threads attendent que d'autres décomptent. Barrière : réutilisable, les mêmes threads s'attendent entre eux.
8. **Pourquoi `wait()` dans un `while` ?** La condition peut être redevenue fausse au réveil (autre thread passé avant, réveil intempestif).
9. **Pourquoi préférer `BlockingQueue` à `wait/notify` ?** Moins de code, moins de bugs, contre-pression native, timeouts, arrêt propre.
10. **Quand utiliser un `ReadWriteLock` ou un `StampedLock` ?** Lectures nombreuses et longues, écritures rares ; `StampedLock` seulement si le gain est mesuré (non réentrant, plus délicat).

# Multithreading classique : vue d'ensemble

Package racine : `fr.vt.simulation.threadclassique`

Ce document résume les six fiches de révision du projet et donne un **ordre d'apprentissage global**. Chaque section dit : le problème, les outils, ce qu'il faut retenir, et renvoie à la fiche détaillée.

| Thème | Package | Fiche détaillée |
|---|---|---|
| Synchronisation (race condition, `synchronized`, synchroniseurs) | `synchroniseurs.*` (de `etape0_race` à `etape5_exchanger`) | [`README-synchroniseurs.md`](README-synchroniseurs.md) |
| Deadlock (détection, ordre global, `tryLock`) | `deadlock.*` (de `etape1_deadlock` à `etape3_trylock`) | [`README-deadlock.md`](README-deadlock.md) |
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
| 2 | **Deadlock** | Le revers des verrous : dès qu'un thread en prend **plusieurs**, il risque l'interblocage. On le provoque, on le détecte, puis on le corrige (ordre global, `tryLock`). |
| 3 | **Structures de données** | Suite directe de la race condition : `AtomicInteger` la corrige sans verrou, puis le CAS sur un objet, puis `ConcurrentHashMap`. |
| 4 | **Pools et futures** | Après avoir compris la coordination, on cesse de créer des threads à la main : on les confie à un pool. |
| 5 | **Producteur / consommateur** | Premier schéma classique, d'abord à la main (`wait/notify`), puis avec une `BlockingQueue`. |
| 6 | **Lecteurs / rédacteurs** | Schéma où les lectures dominent : verrous de lecture et d'écriture, puis lecture optimiste. |

Pas encore couverts par une démo : tous les sujets **virtual threads**.

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

## 2. Deadlock

Fiche détaillée : [`README-deadlock.md`](README-deadlock.md)

**Problème.** Deux threads prennent les mêmes verrous dans l'ordre inverse : chacun détient le sien et attend celui de l'autre, pour toujours. Fil conducteur : deux virements croisés entre les comptes A et B.

| Étape | Package | Démo | Idée |
|---|---|---|---|
| 1 | `etape1_deadlock` | `DeadlockDemo` | **Le problème** : deux `synchronized` imbriqués en ordre inverse. Détection par `ThreadMXBean.findDeadlockedThreads()`. |
| 2 | `etape2_lockordering` | `LockOrderingDemo` | **Ordre global** : toujours verrouiller le plus petit id d'abord. Plus d'attente circulaire. |
| 3 | `etape3_trylock` | `TryLockDemo` | **`tryLock(délai)`** : on renonce, on relâche, on réessaie après un délai aléatoire (anti-livelock). |

**À retenir**
- Il faut les **4 conditions de Coffman** (exclusion mutuelle, détention et attente, pas de préemption, attente circulaire) ; en casser **une** suffit.
- Symptôme : threads `BLOCKED`, CPU à 0 %. Diagnostic : `jstack <pid>`, `jcmd <pid> Thread.print`, ou `findDeadlockedThreads()`.
- Un thread bloqué sur `synchronized` ne peut pas être débloqué ; avec `ReentrantLock`, `tryLock(délai)` et `lockInterruptibly()` permettent d'en sortir.
- Ne pas appeler de code inconnu en détenant un verrou.

---

## 3. Structures de données thread-safe

Fiche détaillée : [`README-structures-de-donnees.md`](README-structures-de-donnees.md)

**Problème.** Modifier une donnée partagée **sans perdre d'écriture**, avec ou sans verrou.

| Étape | Package | Démo | Idée |
|---|---|---|---|
| 1 | `etape1_atomicinteger` | `AtomicIntegerDemo` | Compteur **atomique** sans verrou, basé sur le **CAS** (Compare-And-Swap). |
| 2 | `etape2_atomiclong` | `AtomicLongDemo` | `AtomicLong` vs `LongAdder` : même résultat, mais sous forte contention `LongAdder` est 5 à 10 fois plus rapide (écritures réparties sur plusieurs cellules). |
| 3 | `etape3_atomicreference` | `AtomicReferenceDemo` | CAS sur un **objet** : `compareAndSet(attendu, nouveau)` ne réussit que pour un seul thread ; schéma lire, calculer, `compareAndSet`, recommencer en cas d'échec. |
| 4 | `etape4_concurrenthashmap` | `ConcurrentHashMapDemo` | Une `HashMap` perd des clés même quand elles sont distinctes ; `ConcurrentHashMap` n'en perd aucune. |

**À retenir**
- CAS : « écrire seulement si la valeur est toujours celle que j'ai lue, sinon recommencer ». Aucun thread n'est bloqué.
- Compteur très disputé et rarement lu : `LongAdder` ; besoin de `compareAndSet` ou d'une valeur exacte à tout instant : `AtomicLong`.
- `AtomicReference` compare par **référence** (`==`) ; attention au problème **ABA**.
- Atomique = **une seule variable**. Plusieurs variables liées demandent un verrou.
- `ConcurrentHashMap` protège **chaque appel**, pas une séquence : jamais `get` puis `put` ; utiliser `merge`, `compute`, `putIfAbsent`.

---

## 4. Pools de threads et futures

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

## 5. Producteur / consommateur

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

## 6. Lecteurs / rédacteurs

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
| Prendre plusieurs verrous sans deadlock | Ordre global des verrous, sinon `ReentrantLock.tryLock(délai)` |
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

Questions fréquentes, regroupées par thème, avec une réponse type. Les renvois entre parenthèses indiquent la démo ou la fiche qui l'illustre.

### A. Fondamentaux

1. **Quelle est la différence entre un processus et un thread ?**
   Un processus a son propre espace mémoire et ses ressources (isolé des autres). Les threads d'un même processus **partagent** la mémoire (le tas) mais ont chacun leur pile et leur compteur d'instruction. Partager la mémoire rend les threads légers et rapides à communiquer, mais crée les problèmes de concurrence (race conditions, visibilité).

2. **Quelle est la différence entre `start()` et `run()` ?**
   `start()` crée un **nouveau thread** qui exécute `run()`. Appeler `run()` directement exécute le code dans le thread **appelant**, sans aucun parallélisme. Appeler `start()` deux fois lève `IllegalThreadStateException`.

3. **Qu'est-ce qu'une race condition ?**
   Plusieurs threads lisent et modifient une donnée partagée sans coordination ; le résultat dépend de l'ordre d'entrelacement, donc il est imprévisible. Exemple : `counter++` fait trois étapes (lire, ajouter, écrire) et perd des incréments (`RaceConditionDemo`). Elle est **probabiliste** : un test qui passe ne prouve rien.

4. **Quels sont les trois problèmes de la concurrence en Java ?**
   **Atomicité** (une opération composée peut être entrecoupée, comme `counter++`), **visibilité** (un thread peut ne pas voir l'écriture d'un autre, à cause des caches et du réordonnancement) et **ordre** (le compilateur et le processeur peuvent réordonner les instructions). `synchronized`, `volatile` et les atomiques traitent ces problèmes à des degrés différents.

5. **Que fait `volatile`, et qu'est-ce qu'il ne fait pas ?**
   Il garantit la **visibilité** (une écriture est vue par les lectures suivantes) et interdit certains réordonnancements. Il ne rend **pas atomique** une opération composée : `volatile int x; x++` perd toujours des incréments. À utiliser pour un indicateur simple (`volatile boolean arret`), pas pour un compteur.

6. **Qu'est-ce que la relation « happens-before » ?**
   Une garantie du modèle mémoire Java : si l'action A *happens-before* B, alors B voit les effets de A. Exemples : la libération d'un verrou précède son acquisition suivante ; une écriture `volatile` précède la lecture suivante ; `start()` précède le début du thread ; la fin d'un thread précède le retour de son `join()`. C'est pourquoi `main` voit les écritures des threads après `join()`.

7. **Qu'est-ce qu'un deadlock, et comment l'éviter ?**
   Des threads s'attendent mutuellement pour toujours. Quatre conditions réunies : **exclusion mutuelle**, **détention et attente** (on garde un verrou en en demandant un autre), **pas de préemption** et **attente circulaire**. Pour l'éviter : prendre les verrous toujours dans le **même ordre**, réduire le nombre de verrous, utiliser `tryLock(délai)` pour abandonner, ne pas appeler de code inconnu sous verrou. Un `jstack` ou un `jcmd <pid> Thread.print` signale les deadlocks. (`DeadlockDemo`, `LockOrderingDemo`, `TryLockDemo`)

8. **Quelle différence entre deadlock, livelock et famine ?**
   Deadlock : les threads sont **bloqués** pour toujours. Livelock : ils **travaillent** sans avancer (ils se cèdent mutuellement le passage et recommencent). Famine : un thread n'obtient jamais la ressource parce que d'autres passent toujours avant (par exemple un rédacteur face à un flot de lecteurs).

9. **Qu'est-ce qu'un thread daemon ?**
   Un thread de service que la JVM n'attend pas : la JVM s'arrête quand il ne reste que des threads daemon. Les threads d'un pool fixe sont **non daemon** (sans `shutdown()` le programme ne se termine pas) ; ceux du pool commun de `CompletableFuture` sont **daemon** (sans `join()` la tâche peut ne jamais s'exécuter).

10. **Comment fonctionne l'interruption d'un thread ?**
    Elle est **coopérative** : `interrupt()` positionne un indicateur, il n'arrête pas le thread. Les méthodes bloquantes (`sleep`, `wait`, `take`, `await`…) lèvent `InterruptedException` et **effacent** l'indicateur. Bonne pratique : ne jamais ignorer l'exception ; soit la propager, soit restaurer le statut avec `Thread.currentThread().interrupt()` avant de sortir.

11. **Quelle différence entre `sleep()`, `wait()` et `join()` ?**
    `sleep(ms)` suspend le thread et **ne relâche aucun verrou**. `wait()` (dans un bloc `synchronized`) **relâche le verrou** et attend un `notify`. `join()` attend la **fin** d'un autre thread.

12. **Qu'est-ce qu'une classe thread-safe, et comment en obtenir une ?**
    Une classe qui reste correcte sous accès concurrents sans synchronisation supplémentaire de l'appelant. Par ordre de préférence : **ne pas partager** (données locales au thread), objets **immuables**, structures **concurrentes** de la bibliothèque, puis synchronisation explicite.

### B. Synchronisation

13. **`synchronized`, atomique ou verrou explicite : comment choisir ?**
    Une seule variable simple : un **atomique**. Plusieurs variables qui doivent rester cohérentes : `synchronized` ou un verrou. Besoin de `tryLock`, de timeout, d'interruption ou d'équité : verrou explicite (`ReentrantLock`).

14. **Que garantit `synchronized` ?**
    L'**exclusion mutuelle** (un seul thread à la fois par verrou) et la **visibilité** (les écritures faites sous le verrou sont vues par le thread suivant qui prend le même verrou). Sur une méthode d'instance le verrou est `this`, sur une méthode `static` c'est la classe. Il est **réentrant** et libéré automatiquement, même en cas d'exception.

15. **`synchronized` ou `ReentrantLock` ?**
    `ReentrantLock` ajoute `tryLock()`, le timeout, `lockInterruptibly()`, le mode équitable et plusieurs `Condition`, au prix d'un `unlock()` **obligatoire dans un `finally`**. `synchronized` est plus simple et plus sûr ; on préfère donc `synchronized` tant qu'on n'a pas besoin de ces fonctions.

16. **Quelle différence entre un `Semaphore` et `synchronized` ?**
    `synchronized` laisse entrer **un** thread et a un **propriétaire** (seul le détenteur libère). `Semaphore` laisse entrer **N** threads et n'a **pas de propriétaire** : n'importe quel thread peut faire `release()`, ce qui peut créer des permis en trop. `release()` va dans un `finally`. (`SemaphoreParkingDemo`)

17. **`CountDownLatch` ou `CyclicBarrier` ?**
    Latch : **usage unique**, des threads attendent que d'autres décomptent (ceux qui décomptent ne sont pas bloqués). Barrière : **réutilisable**, les mêmes N threads s'attendent entre eux, avec une action de barrière. Un thread qui échoue **casse** la barrière (`BrokenBarrierException`) ; si un ouvrier oublie `countDown()`, le latch bloque `await()` pour toujours (d'où le `finally`). (`CountDownLatchDemo`, `CyclicBarrierDemo`)

18. **À quoi sert un `Exchanger` ?**
    À échanger une donnée entre **exactement deux** threads à un point de rendez-vous. Cas typique : le *double buffering* (un thread remplit un tampon pendant que l'autre vide le précédent, puis ils échangent). (`ExchangerDemo`)

19. **Pourquoi `wait()` dans un `while` et non dans un `if` ?**
    Au réveil, la condition peut être redevenue fausse : un autre thread est passé avant, ou le réveil est **intempestif** (*spurious wakeup*, permis par la spécification). Avec `while`, on retest la condition à chaque réveil.

20. **`notify()` ou `notifyAll()` ?**
    `notifyAll()` par défaut. Avec `notify()`, on réveille un seul thread au hasard, qui peut être le mauvais (un consommateur alors que le tampon est vide) : le programme se bloque. `wait`, `notify` et `notifyAll` exigent de détenir le moniteur, sinon `IllegalMonitorStateException`.

### C. Structures de données et atomiques

21. **Qu'est-ce que le CAS (Compare-And-Swap) ?**
    Une instruction atomique du processeur : « écrire la nouvelle valeur **seulement si** la valeur actuelle est celle attendue, sinon échouer ». On l'utilise en **boucle** : lire, calculer, `compareAndSet`, recommencer en cas d'échec. Aucun thread n'est bloqué (algorithmes *lock-free*). (`AtomicIntegerDemo`, `AtomicReferenceDemo`)

22. **Qu'est-ce que le problème ABA ?**
    La valeur passe de A à B puis revient à A entre la lecture et le CAS : le CAS réussit sans voir le changement intermédiaire. Solution : ajouter un numéro de version avec `AtomicStampedReference`. De plus `compareAndSet` sur une référence compare par `==` (identité), pas avec `equals()`.

23. **`AtomicInteger`, `synchronized` ou `LongAdder` pour un compteur ?**
    `AtomicInteger` : sans verrou, adapté à **une** variable. `synchronized` : si plusieurs variables doivent rester cohérentes. `LongAdder` : sous **forte contention** (métriques), il répartit les écritures sur plusieurs cellules et est plus rapide, mais `sum()` n'est pas un instantané atomique.

24. **Pourquoi une `HashMap` perd-elle des données avec des clés différentes ?**
    Les threads partagent sa **structure interne** (tableau de cases, chaînage, redimensionnement) : deux `put()` simultanés peuvent écraser le chaînage l'un de l'autre, ou tomber pendant un redimensionnement dont les entrées recopiées disparaissent. (`ConcurrentHashMapDemo`)

25. **`ConcurrentHashMap` est thread-safe : `get()` puis `put()` est-il sûr ?**
    Non. Chaque appel est atomique, pas l'**enchaînement** (*check-then-act*) : deux threads peuvent lire la même valeur et l'un écrase l'autre. Utiliser les opérations atomiques `merge`, `compute`, `computeIfAbsent`, `putIfAbsent`, par exemple `map.merge(clé, 1, Integer::sum)` pour compter.

26. **`ConcurrentHashMap`, `Hashtable` ou `Collections.synchronizedMap` ?**
    `Hashtable` et `synchronizedMap` utilisent **un seul verrou** pour toute la map : tout est sérialisé. `ConcurrentHashMap` verrouille **par case** (et utilise le CAS), lit **sans verrou** et se redimensionne à plusieurs threads : bien plus scalable. Particularités : ni clé ni valeur `null`, `size()` approximatif, itérateurs « faiblement cohérents » (pas de `ConcurrentModificationException`).

### D. Pools de threads et futures

27. **Pourquoi un pool plutôt qu'un thread par tâche ?**
    Créer un thread coûte cher (mémoire, ordonnancement) et ne plafonne pas la concurrence. Un pool **réutilise** N threads, borne le parallélisme et sépare *quoi faire* (la tâche) de *comment l'exécuter*. (`FixedThreadPoolDemo`)

28. **Que se passe-t-il sans `shutdown()` ? Et quelle est la différence avec `shutdownNow()` ?**
    Sans `shutdown()`, les threads du pool (non daemon) gardent la JVM en vie : le programme ne se termine pas. `shutdown()` refuse les nouvelles tâches et laisse finir celles déjà soumises ; `shutdownNow()` tente d'**interrompre** les tâches en cours et renvoie la liste de celles qui n'ont pas démarré ; `awaitTermination(délai)` attend la fin effective.

29. **Quel est le piège de `Executors.newFixedThreadPool` ?**
    Sa file d'attente est **non bornée** : si les tâches arrivent plus vite qu'elles ne sont traitées, la file grossit jusqu'à `OutOfMemoryError`. En production, on crée un `ThreadPoolExecutor` avec une file **bornée** et une politique de rejet explicite.

30. **Quels sont les paramètres d'un `ThreadPoolExecutor` ?**
    `corePoolSize` (threads gardés), `maximumPoolSize`, `keepAliveTime`, la **file de travail**, une `ThreadFactory` (noms des threads) et un `RejectedExecutionHandler`. Ordre de remplissage : d'abord les threads *core*, puis la **file**, puis seulement des threads supplémentaires jusqu'au maximum, puis le rejet. Avec une file non bornée, le maximum n'est jamais atteint.

31. **Comment dimensionner un pool ?**
    Tâches **CPU** : à peu près le nombre de cœurs (`availableProcessors()`, parfois +1). Tâches **bloquantes** (réseau, disque, BDD) : plus de threads, environ `cœurs × (1 + temps d'attente / temps de calcul)`, à **mesurer**. Utiliser un pool distinct par type de travail.

32. **Quelle différence entre `submit()` et `execute()` ?**
    `submit()` renvoie un `Future` et **capture** l'exception dans le `Future` (perdue si personne n'appelle `get()`). `execute()` prend un `Runnable` et laisse l'exception remonter au thread.

33. **`Runnable` ou `Callable` ?**
    `Callable` renvoie une valeur et peut lever une exception vérifiée ; `Runnable` ne renvoie rien. `submit(callable)` donne un `Future<V>`. (`CallableFutureDemo`)

34. **`Future` ou `CompletableFuture` ?**
    `Future.get()` **bloque** et on ne peut ni chaîner ni combiner sans bloquer. `CompletableFuture` permet de chaîner (`thenApply`, `thenAccept`, `thenRun`), combiner (`thenCombine`, `allOf`), gérer les erreurs (`exceptionally`, `handle`) et poser un timeout, sans bloquer entre les étapes.

35. **`get()` ou `join()` ?**
    Les deux attendent le résultat. `get()` lève des exceptions **vérifiées** (`InterruptedException`, `ExecutionException`) ; `join()` lève `CompletionException`, **non vérifiée**, plus pratique dans les lambdas. Dans les deux cas, la cause d'origine est dans `getCause()`.

36. **`thenApply` ou `thenCompose` ?**
    `thenApply` quand la fonction renvoie une **valeur** (comme `map`). `thenCompose` quand elle renvoie elle-même un `CompletableFuture` : il « aplatit » et évite un `CompletableFuture<CompletableFuture<T>>` (comme `flatMap`). `thenAccept` consomme sans rien renvoyer ; `thenRun` réagit à la fin sans recevoir le résultat. (`CompletableFutureChainingDemo`)

37. **Que se passe-t-il si une étape d'une chaîne `CompletableFuture` échoue ?**
    Les étapes suivantes sont **sautées** et l'erreur ressort au `join()` (en `CompletionException`), sauf si `exceptionally` (valeur de secours) ou `handle` (résultat ou erreur) la traite avant. Sans `join()` ni traitement, l'échec reste **silencieux**.

38. **Quel pool utilise `supplyAsync` par défaut, et quel est le risque ?**
    Le `ForkJoinPool.commonPool()` : petit (environ cœurs − 1), **partagé** par toute l'application (et les streams parallèles) et composé de threads **daemon**. Il ne faut pas y mettre de tâches bloquantes : passer un `Executor` dédié en second paramètre.

### E. Schémas classiques

39. **Pourquoi préférer une `BlockingQueue` à `wait/notify` ?**
    Moins de code, moins de bugs (pas de `while`/`notifyAll` à gérer), **contre-pression** native avec une file bornée, timeouts (`offer`, `poll`) et arrêt propre. `put()` bloque si la file est pleine, `take()` si elle est vide. (`BlockingQueueProducerConsumerDemo`)

40. **Comment arrête-t-on proprement un consommateur ?**
    Avec une **poison pill** (valeur spéciale envoyée dans la file, une **par consommateur**), par interruption du thread, ou avec un `poll(délai)` combiné à un indicateur `volatile`. Si le producteur plante avant d'envoyer la pill, le consommateur reste bloqué : prévoir un timeout.

41. **Qu'est-ce que la contre-pression ?**
    Le mécanisme qui **freine** le producteur quand le consommateur ne suit pas. Avec une file **bornée**, `put()` bloque quand elle est pleine, ce qui évite d'accumuler les éléments en mémoire.

42. **Quand utiliser un `ReadWriteLock` ou un `StampedLock` ?**
    Lectures **nombreuses et longues**, écritures rares. `ReentrantReadWriteLock` : plusieurs lecteurs **ou** un seul rédacteur, réentrant. `StampedLock` : ajoute la **lecture optimiste** (lire sans verrou puis `validate()`), plus rapide mais **non réentrant**, sans `Condition` et plus délicat : à n'utiliser que si le gain est **mesuré**. Sinon un simple `synchronized` suffit. (`ReadWriteLockDemo`, `StampedLockDemo`)

43. **Peut-on passer d'un verrou de lecture à un verrou d'écriture ?**
    Avec `ReentrantReadWriteLock`, **non** : un thread qui détient le verrou de lecture et demande celui d'écriture se bloque lui-même (deadlock). L'inverse (rétrogradation écriture vers lecture) est permis. Avec `StampedLock`, `tryConvertToWriteLock` permet la conversion.

### F. En pratique

44. **Comment diagnostiquer un problème de concurrence ?**
    Donner des **noms** aux threads (utiles dans les logs), prendre un **thread dump** (`jstack <pid>` ou `jcmd <pid> Thread.print`) pour repérer un deadlock ou des threads bloqués, répéter les tests avec beaucoup d'itérations, et raisonner sur le code : une race condition est probabiliste, un test qui passe ne prouve rien.

45. **Quelles règles générales pour écrire du code concurrent fiable ?**
    Ne pas partager quand on peut (confinement, immuabilité) ; préférer les outils de haut niveau (`ExecutorService`, `BlockingQueue`, `ConcurrentHashMap`, atomiques) aux `wait/notify` ; libérer dans un **`finally`** (`unlock`, `release`, `countDown`, `shutdown`) ; toujours un **timeout** quand on attend un système externe ; ne pas ignorer `InterruptedException` ; garder les sections critiques courtes, sans I/O dedans.

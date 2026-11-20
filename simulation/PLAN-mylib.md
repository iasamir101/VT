# Plan : bibliothèque maison `mylib` (réimplémenter les outils de concurrence)

## Contexte
Tu as fini la partie classique du projet en **utilisant** les outils de `java.util.concurrent`. L'étape suivante consiste à les **réimplémenter**. C'est le meilleur moyen de comprendre CAS, attente, réveil et contre-pression, et c'est un exercice d'entretien classique (« code-moi un sémaphore / une file bloquante »).

Le dossier `threadclassique/mylib/` existe déjà mais il est vide. Tes choix :
- **Squelette + toi** : j'écris les classes avec la Javadoc, les signatures, des TODO avec indices et des démos auto-vérifiantes. Tu écris les corps des méthodes, puis je relis.
- **Couches bas niveau** : chaque classe repose sur la précédente, sans utiliser l'outil JDK qu'elle remplace.

## Organisation
- Package `fr.vt.simulation.threadclassique.mylib` (on garde `mylib` en minuscules, comme le dossier existant et la convention Java). Les classes y sont **publiques**, puisqu'elles seront réutilisées ailleurs.
- Une démo par étape, dans un sous-package `mylib.etapeN_xxx`. Chaque démo importe la classe de la bibliothèque et applique le format Javadoc habituel (But / Entrée / Sortie / sortie attendue / À observer / Lancer).
- Avant ton implémentation, chaque méthode à écrire fait `throw new UnsupportedOperationException("TODO")`. Le code compile donc tout de suite.
- Chaque démo **se vérifie elle-même** : elle teste un invariant et affiche `OK` ou `ÉCHEC : ...`. Tu sais ainsi si ton implémentation est juste.

## Étapes (dans l'ordre de difficulté)

> **État** : seule l'étape 1 est créée. Les étapes 2 à 4 sont reportées à plus tard (décision de l'auteur) ; on les créera à la demande.

### Étape 1 : `MyAtomicPairInteger`, un `AtomicInteger` qui ne contient que des nombres pairs
- Valeurs possibles : 0, 2, 4… Un incrément ajoute 2, un décrément retire 2, une valeur impaire est refusée (`IllegalArgumentException`).
- Interne : `volatile int value`, écritures dans des méthodes `synchronized`, lecture `get()` sans verrou (la version `VarHandle` a été retirée).
- API : `get()`, `compareAndSet`, `incrementAndGet`, `getAndIncrement`, `decrementAndGet`, `addAndGet`. **Fait** : la démo affiche OK.
- Démo `etape1_atomicpair.AtomicPairDemo` : 8 threads font 10 000 incréments pendant qu'un lecteur vérifie en continu que la valeur est paire. On compare avec `NaiveEvenCounter` (+1 puis +1 sur un `AtomicInteger`), où des valeurs impaires sont vues. Viennent ensuite des tests de l'API (valeurs impaires refusées, CAS, course entre deux CAS).
- Leçon : un invariant se protège en un seul CAS ; `volatile` donne la visibilité, pas l'atomicité.

### Étape 2 : `MyLocker` + `MyCondition`, un verrou à toi
- Interne : `AtomicReference<Thread> owner`, plus une file de threads en attente (`ConcurrentLinkedQueue<Thread>`) et `LockSupport.park/unpark`. C'est le modèle `FIFOMutex` de la Javadoc de `LockSupport`.
- API : `lock()`, `unlock()` (lève `IllegalMonitorStateException` si l'appelant n'est pas le propriétaire), `boolean tryLock()`, `MyCondition newCondition()`.
- `MyCondition` : `await()` (s'inscrire, libérer le verrou, se garer, puis reprendre le verrou), `signal()` et `signalAll()`. Les réveils intempestifs sont gérés par une boucle `while` côté appelant.
- Progression suggérée dans les TODO : v1 = verrou à attente active (`while(!CAS) Thread.onSpinWait()`), v2 = threads garés.
- Démo `etape2_locker.MyLockerDemo` : un compteur incrémenté 4 × 100 000 fois sous `MyLocker` doit afficher 400 000, plus un test `tryLock` et un test d'`unlock` par un mauvais thread.

### Étape 3 : `MySemaphore`, construit sur `MyLocker` + `MyCondition`
- API : `MySemaphore(int permits)`, `acquire()`, `release()`, `boolean tryAcquire()` et `int availablePermits()`.
- `acquire` : `lock` ; `while (permits == 0) permitsAvailable.await()` ; `permits--` ; `unlock`.
- Démo `etape3_semaphore.MySemaphoreParkingDemo` : même scénario que `synchroniseurs/etape1_semaphore/SemaphoreParkingDemo` (parking à 3 places, 6 voitures). Un `AtomicInteger` mesure le maximum de voitures garées en même temps, qui doit être ≤ 3.

### Étape 4 : `MyConcurrentQueue<T>`, une file bloquante bornée
- Interne : un tableau circulaire (`head`, `tail`), plus le schéma classique des **deux sémaphores + un mutex** : `MySemaphore emptySlots(capacity)`, `MySemaphore filledSlots(0)`, `MyLocker mutex`.
- API : `put(T)` (bloque si la file est pleine), `T take()` (bloque si elle est vide), `int size()`.
- Démo `etape4_queue.MyQueueProducerConsumerDemo` : reprend `producteurconsommateur/etape2_blockingqueue` (producteur, consommateur, poison pill `-1`, capacité 5), avec `MyConcurrentQueue` à la place d'`ArrayBlockingQueue`. Vérification : somme reçue = somme envoyée, sans perte ni doublon. Variante : 2 producteurs et 2 consommateurs.

### Suite possible (pas dans ce lot)
`MyCountDownLatch`, `MyReadWriteLock`, `MyThreadPool` (des workers qui consomment une `MyConcurrentQueue<Runnable>`) et `MyCyclicBarrier`. On fera un plan par classe le moment venu.

## Fichiers à créer
- `simulation/src/main/java/fr/vt/simulation/threadclassique/mylib/` : `MyAtomicPairInteger.java`, `MyLocker.java`, `MyCondition.java`, `MySemaphore.java`, `MyConcurrentQueue.java`.
- `mylib/etape1_atomicpair/AtomicPairDemo.java` (+ classe naïve package-private), `mylib/etape2_locker/MyLockerDemo.java`, `mylib/etape3_semaphore/MySemaphoreParkingDemo.java` et `mylib/etape4_queue/MyQueueProducerConsumerDemo.java` (+ `Producer` / `Consumer` package-private).
- `simulation/README-mylib.md` : la fiche d'étude en français, avec une section par étape (notion, schéma d'implémentation, pièges : réveil intempestif, signal perdu, ordre acquire/lock dans la file, questions d'entretien).
- Mises à jour : `simulation/README-thread-classique.md` (nouvelle partie « mylib ») et `CLAUDE.md` (layout + liste des README).

## Réutilisation
- Le scénario du parking vient de `synchroniseurs/etape1_semaphore/SemaphoreParkingDemo.java`.
- La structure producteur / consommateur et la poison pill viennent de `producteurconsommateur/etape2_blockingqueue/` (`Producer`, `Consumer`, `POISON_PILL`).

## Vérification
1. Depuis `simulation/` : `mvn compile` doit passer avec les squelettes.
2. `mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.mylib.etape1_atomicpair.AtomicPairDemo` doit afficher `UnsupportedOperationException: TODO` avant ton implémentation, puis `OK` après.
3. Même chose pour les démos des étapes 2 à 4. Lance chaque démo plusieurs fois, car les bugs de concurrence sont intermittents.
4. Quand une étape affiche `OK`, tu me demandes une relecture de ton implémentation.

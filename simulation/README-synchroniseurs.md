# Synchroniseurs : fiches de révision

Package racine : `fr.vt.simulation.threadclassique.synchroniseurs` (les packages `etape1_semaphore` à `etape5_exchanger` ci-dessous en sont des sous-packages ; `RaceConditionDemo` est dans `threadclassique.race`).

Ces outils **coordonnent des threads entre eux** : exclusion mutuelle, limitation d'accès, attente de la fin des autres, rendez-vous, échange de données. Chaque fiche suit le même plan : notion, déroulement, avantages, limites, astuces, bonnes pratiques, à retenir.

| Étape | Package | Démo | Question à laquelle il répond |
|---|---|---|---|
| départ | `threadclassique.race` | `RaceConditionDemo` | Quel est le problème que les synchroniseurs résolvent ? |
| 1 | `etape1_semaphore` | `SemaphoreParkingDemo` | Combien de threads peuvent entrer en même temps ? |
| 2 | `etape2_synchronized` | `SynchronizedCounterDemo` | Comment n'avoir qu'un seul thread à la fois dans une section ? |
| 3 | `etape3_countdownlatch` | `CountDownLatchDemo` | Comment attendre que N événements soient terminés ? |
| 4 | `etape4_cyclicbarrier` | `CyclicBarrierDemo` | Comment faire attendre N threads les uns les autres ? |
| 5 | `etape5_exchanger` | `ExchangerDemo` | Comment deux threads s'échangent-ils une donnée ? |

Lancer une démo (depuis `simulation/`) :

```bash
mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.synchroniseurs.<package>.<Demo>
```

Fil conducteur : **voir le problème** (départ), **limiter l'accès** (0 et 1), **attendre les autres** (2), **se rejoindre** (3), **échanger** (4).

---

## Point de départ : `RaceConditionDemo` (la race condition)

Cette démo est dans le package `threadclassique.race`, pas dans `synchroniseurs`. Elle montre le **problème** que `synchronized` (étape 2) corrige : `SynchronizedCounterDemo` est exactement la même démo, protégée.

### Notion
Une **race condition** apparaît quand plusieurs threads lisent et modifient la même donnée partagée sans coordination, et que le résultat dépend de l'ordre dans lequel ils s'entrelacent. `counter++` semble une seule opération, mais c'en sont **trois** : lire, ajouter 1, écrire. Ce n'est pas atomique.

### Déroulement
1. Deux threads font chacun **1 000 000** d'incréments sur le même champ statique `counter`, sans aucune protection.
2. Un thread lit `counter` (par exemple 41). Avant qu'il écrive, l'autre lit aussi 41.
3. Les deux écrivent 42 : **un incrément est perdu**.
4. Après les `join()`, `main` compare la valeur attendue (2 000 000) à la valeur obtenue.
5. Résultat : `Attendu = 2000000, obtenu = 1042884` par exemple. La valeur est **inférieure** et **change à chaque exécution**.

### Pourquoi 1 000 000 d'itérations
Avec 1 000 itérations, le premier thread finit souvent avant que le second démarre : les deux ne se marchent jamais dessus et le bug reste invisible. Il faut beaucoup d'opérations pour que les threads s'entrelacent vraiment.

### Avantages (de la démo)
- Rend le bug **visible** avec un code de quelques lignes.
- Sert de référence : on compare le résultat à celui de `SynchronizedCounterDemo` (exact), puis à celui d'`AtomicIntegerDemo` (exact, sans verrou).

### Limites
- C'est **probabiliste** : un résultat exact sur une exécution ne prouve pas que le code est correct.
- Le résultat dépend de la machine, du nombre de cœurs et de la charge : sur certaines machines, l'écart est plus faible.
- Un `println` dans la boucle changerait le comportement (il est synchronisé et ralentit tellement les threads qu'ils s'entrelacent moins).

### Astuces
- Si le résultat est exact par hasard, augmenter le nombre d'itérations.
- Relancer plusieurs fois : les valeurs obtenues sont toutes différentes.
- Ce schéma s'appelle **check-then-act** ou **read-modify-write** : on le retrouve dans les maps (`get` puis `put`).

### Bonnes pratiques
- Toute donnée **partagée et modifiable** doit être protégée : `synchronized`, verrou, variable atomique, ou structure thread-safe.
- Mieux vaut ne pas partager : données locales au thread, ou objets immuables.
- Un bug de concurrence qui « ne se reproduit pas » n'est pas absent : raisonner sur le code plutôt que sur un test qui passe.

### À retenir
> `counter++` = **lire, ajouter, écrire** : trois étapes, donc pas atomique. Sans protection, des incréments sont **perdus**, de façon imprévisible. Solutions : `synchronized` (étape 2), atomiques (`AtomicIntegerDemo`).

---

## 1. `SemaphoreParkingDemo` : limiter le nombre d'accès simultanés

### Notion
Un `Semaphore` est un **compteur de permis**. `acquire()` prend un permis (ou bloque s'il n'en reste aucun), `release()` en rend un. Avec `new Semaphore(3)`, trois threads au plus sont dans la zone protégée en même temps.

### Déroulement
1. Un parking de **3 places** (3 permis) et **10 voitures** (threads).
2. Chaque voiture affiche « attend une place » puis appelle `acquire()`.
3. Trois voitures obtiennent un permis, entrent, se garent 2 s puis quittent.
4. Chaque sortie fait `release()` dans un `finally` : une voiture en attente peut entrer.
5. Résultat : jamais plus de 3 voitures entre un « entre » et son « quitte », en **4 vagues** de 2 s, soit environ **8 s** au total.

### Avantages
- Limite la concurrence sur une ressource rare (connexions, licences, appels vers un service externe).
- Réutilisable : les permis sont rendus et repris en continu.
- Simple : deux méthodes à retenir.

### Limites
- **Aucune notion de propriétaire** : n'importe quel thread peut faire `release()`, même sans avoir fait `acquire()`. Cela crée des permis en trop, sans erreur.
- Non réentrant : un thread qui refait `acquire()` consomme un deuxième permis.
- Ne protège pas les données : il limite le *nombre* de threads, pas ce qu'ils font à l'intérieur. Avec plus d'un permis, il faut encore protéger l'état partagé.
- Un `release()` oublié (exception) perd un permis définitivement.

### Astuces
- `new Semaphore(n, true)` : mode **équitable** (premier arrivé, premier servi), au prix du débit.
- `tryAcquire()` / `tryAcquire(délai, unité)` : ne pas bloquer indéfiniment.
- `Semaphore(1)` se comporte comme un verrou, mais sans propriétaire ni réentrance.
- `availablePermits()` : utile pour le débogage, pas pour décider (valeur périmée aussitôt lue).

### Bonnes pratiques
- **`release()` toujours dans un `finally`**, juste après un `acquire()` réussi (la démo le fait).
- Faire `acquire()` *avant* le `try`, pas dedans : si `acquire()` échoue, on ne doit pas rendre un permis qu'on n'a pas pris.
- Prévoir un timeout (`tryAcquire`) pour les ressources externes.

### À retenir
> Semaphore = **compteur de permis**. `acquire` / `release` dans un `finally`. Il limite la concurrence, il ne protège pas les données et n'a pas de propriétaire.

---

## 2. `SynchronizedCounterDemo` : l'exclusion mutuelle avec `synchronized`

### Notion
`synchronized` utilise le **verrou intrinsèque** (moniteur) d'un objet : un seul thread à la fois peut entrer dans les blocs ou méthodes protégés par le même verrou. Il garantit deux choses : l'**exclusion mutuelle** et la **visibilité** des écritures pour le thread suivant.

### Déroulement
1. C'est le compteur de `RaceConditionDemo`, corrigé : deux threads font chacun 1 000 000 d'appels à `increment()`.
2. `increment()` est `static synchronized` : le verrou est celui de **la classe** (`SynchronizedCounterDemo.class`).
3. Un seul thread à la fois exécute `counter++` (lire, ajouter, écrire) : aucune étape n'est entrecoupée.
4. `join()` fait attendre `main`, qui voit alors toutes les écritures.
5. Résultat : toujours `Attendu = 2000000, obtenu = 2000000`.

### Avantages
- Très simple : un mot-clé, le verrou est libéré automatiquement (même en cas d'exception).
- Réentrant : un thread qui détient déjà le verrou peut le reprendre.
- Garantit aussi la visibilité mémoire (pas besoin de `volatile` en plus pour les données protégées).

### Limites
- **Plus lent que la version non protégée** : les threads s'attendent.
- Pas de timeout, pas de `tryLock()`, pas d'interruption pendant l'attente du verrou.
- Pas d'équité : on ne choisit pas qui entre.
- Un seul verrou par objet : pas de verrous de lecture et d'écriture séparés.
- Risque de **deadlock** si deux threads prennent deux verrous dans un ordre différent.

### Astuces
- `synchronized` sur une méthode d'instance verrouille `this` ; sur une méthode `static`, la classe ; sur un bloc, l'objet donné.
- Pour un simple compteur, `AtomicInteger` évite le verrou (voir `structuresdonnees`).
- Un bloc `synchronized (verrou) { ... }` permet de protéger moins de code qu'une méthode entière.

### Bonnes pratiques
- Garder la section critique **la plus courte possible** : pas de `sleep`, pas d'I/O dedans.
- Verrouiller un objet **privé** (`private final Object verrou = new Object()`), jamais une `String`, un `Integer` ou un objet exposé.
- Toujours prendre plusieurs verrous dans le **même ordre**.
- Protéger *tous* les accès à la donnée (lectures comprises), avec le même verrou.

### À retenir
> `synchronized` = **un seul thread à la fois** + visibilité. Simple et sûr, mais bloquant et sans timeout. Verrou le plus court possible, sur un objet privé.

---

## 3. `CountDownLatchDemo` : attendre la fin de N événements

### Notion
Un `CountDownLatch` est un **compteur à usage unique**. Des threads appellent `countDown()` pour le décrémenter ; ceux qui ont appelé `await()` sont bloqués jusqu'à ce qu'il atteigne **0**. Une fois à 0, il reste ouvert pour toujours.

### Déroulement
1. `new CountDownLatch(3)` : trois événements à attendre.
2. Trois ouvriers démarrent et travaillent 1 s, 2 s, 3 s.
3. `main` affiche « attend les 3 ouvriers... » puis bloque sur `await()`.
4. Chaque ouvrier affiche « a terminé » puis appelle `countDown()`.
5. Au dernier `countDown()` (environ 3 s), `main` est libéré : « tous les ouvriers ont fini ».

### Pourquoi on affiche avant `countDown()`
Après le dernier `countDown()`, `main` est libéré tout de suite et son message peut s'intercaler **avant** celui de l'ouvrier. On affiche donc avant, pour un résultat prévisible.

### Avantages
- Plus souple que `join()` : on n'a pas besoin d'une référence vers chaque thread, et ce n'est pas la *fin* du thread qui compte mais un événement (le thread peut continuer après `countDown()`).
- Les ouvriers ne sont jamais bloqués par `countDown()`.
- Plusieurs threads peuvent attendre le même latch.

### Limites
- **Usage unique** : le compteur ne se remet jamais à zéro. Pour recommencer, il faut un nouveau latch (ou une `CyclicBarrier`).
- Si un ouvrier échoue **avant** son `countDown()`, `await()` ne se termine jamais.
- Sens unique : ceux qui décomptent ne sont pas bloqués et n'attendent pas les autres.

### Astuces
- `await(délai, unité)` : abandonne au bout du délai (renvoie `false` si le compteur n'est pas à 0).
- **Latch de départ** : `new CountDownLatch(1)`. Tous les threads font `await()`, puis `main` fait `countDown()` : ils partent en même temps (utile pour les tests de concurrence).
- `getCount()` : valeur courante, pour le débogage.

### Bonnes pratiques
- Appeler `countDown()` dans un **`finally`**, pour qu'une exception ne bloque pas `await()`.
- Toujours un timeout sur `await()` quand l'attente dépend d'un système externe.
- Restaurer l'interruption si on attrape `InterruptedException`.

### À retenir
> CountDownLatch = **« attends que N choses soient faites »**. Sens unique, **usage unique**. `countDown()` dans un `finally`.

---

## 4. `CyclicBarrierDemo` : se retrouver à chaque étape

### Notion
Une `CyclicBarrier` fait **attendre N threads les uns les autres** : chacun appelle `await()` et reste bloqué jusqu'à ce que les N soient arrivés. Elle se **réinitialise automatiquement** : elle sert pour plusieurs étapes de suite (« cyclique »).

### Déroulement
1. `new CyclicBarrier(3, action)` : trois threads, et une action exécutée à chaque franchissement.
2. Trois threads travaillent de durées différentes (le thread *n* dort *n* secondes), sur plusieurs étapes (constante `STEPS`).
3. Chaque thread affiche « arrive à la barrière » puis appelle `await()` et se bloque.
4. Quand le dernier arrive (le plus lent), l'**action de barrière** s'exécute une seule fois (« >>> Tous arrivés »), dans le thread arrivé en dernier, puis tous repartent ensemble.
5. La barrière est prête pour l'étape suivante : aucun thread ne passe à l'étape N+1 avant que le plus lent ait fini l'étape N.

### Avantages
- Synchronise des phases : idéal pour les algorithmes par étapes (calcul parallèle, simulations).
- Réutilisable, sans recréer d'objet.
- L'action de barrière permet de faire un travail commun (agréger, afficher) entre deux étapes.

### Limites
- Le nombre de threads est **fixe** et doit correspondre exactement au nombre de parties : trop peu de threads, et tout reste bloqué.
- **Barrière cassée** : si un thread est interrompu, a un timeout ou échoue, les autres reçoivent une `BrokenBarrierException` et la barrière est inutilisable jusqu'à `reset()`.
- La vitesse d'une étape est celle du thread le plus lent.

### Astuces
- `await()` renvoie l'**ordre d'arrivée** du thread (0 pour le dernier) : utile pour désigner un thread chargé d'une tâche.
- `await(délai, unité)` évite l'attente infinie (lève `TimeoutException` et casse la barrière).
- `getNumberWaiting()`, `isBroken()` : pour le débogage.

### Bonnes pratiques
- Garder l'action de barrière **courte** : tous les threads attendent pendant qu'elle s'exécute.
- Toujours traiter `BrokenBarrierException` : un thread qui meurt ne doit pas bloquer les autres sans signal.
- Le nombre de threads = le nombre de parties de la barrière.

### Différence avec `CountDownLatch`

| | `CountDownLatch` | `CyclicBarrier` |
|---|---|---|
| Qui attend | un ou plusieurs threads attendent que d'autres décomptent | tous les threads s'attendent entre eux |
| Réutilisable | non | oui (cycles) |
| Les threads qui signalent attendent-ils ? | non (`countDown()` ne bloque pas) | oui (`await()` bloque) |
| Action à la fin | non | oui (`Runnable` de barrière) |
| Échec d'un thread | `await()` peut ne jamais finir | barrière cassée (`BrokenBarrierException`) |

### À retenir
> CyclicBarrier = **« on attend tous les N, puis on repart ensemble »**, à chaque étape. Réutilisable, avec une action de barrière. Un thread qui échoue **casse** la barrière.

---

## 5. `ExchangerDemo` : échanger une donnée à deux

### Notion
Un `Exchanger<V>` est un **point de rendez-vous pour exactement deux threads** : chacun appelle `exchange(sa_donnée)`, attend l'autre, puis reçoit la donnée de son partenaire. L'échange est atomique.

### Déroulement
1. Deux threads : « Rapide » (travaille 1 s) et « Lent » (travaille 3 s), chacun avec sa donnée (`données-rapide`, `données-lent`).
2. Rapide arrive au rendez-vous après 1 s et appelle `exchange()` : il se **bloque**.
3. Lent arrive après 3 s : l'échange a lieu.
4. Les deux repartent en même temps, chacun avec la donnée de l'autre : Rapide a attendu environ 2 s.

### Avantages
- Transfert de données sans file ni verrou à gérer soi-même.
- Synchronisation et échange en un seul appel.
- Cas typique : **double buffering**. Un thread remplit un tampon pendant que l'autre vide le précédent, puis ils échangent leurs tampons.

### Limites
- **Exactement deux threads** par échange. Avec plus de threads, les échanges se font deux à deux, dans un ordre non déterminé.
- Bloque tant que le partenaire n'est pas là : sans partenaire, attente infinie.
- Cas d'usage plus rare que les autres synchroniseurs : peu utilisé en pratique.

### Astuces
- `exchange(valeur, délai, unité)` : lève `TimeoutException` si aucun partenaire n'arrive.
- On peut passer `null` : un thread qui n'a rien à donner peut quand même recevoir.
- Le type de l'échange est générique : `Exchanger<List<String>>`, `Exchanger<byte[]>`, etc.

### Bonnes pratiques
- Toujours un timeout si le partenaire peut disparaître.
- Après l'échange, chaque thread possède désormais la donnée de l'autre : l'ancien propriétaire ne doit plus y toucher.
- Pour un flux continu de messages, préférer une `BlockingQueue` (voir `producteurconsommateur`).

### À retenir
> Exchanger = **« je te donne ça, tu me donnes ça »** entre deux threads, au point de rendez-vous. Bloque jusqu'à l'arrivée du partenaire.

---

## Comparatif : choisir le bon synchroniseur

| Besoin | Outil |
|---|---|
| Un seul thread à la fois dans une section de code | `synchronized` |
| Limiter à N threads l'accès à une ressource | `Semaphore` |
| Attendre que N tâches soient terminées (une fois) | `CountDownLatch` |
| Faire démarrer plusieurs threads exactement en même temps | `CountDownLatch(1)` comme latch de départ |
| Faire avancer N threads par étapes, tous ensemble | `CyclicBarrier` |
| Échange de données entre deux threads au rendez-vous | `Exchanger` |

## Questions d'entretien fréquentes

1. **Différence entre `Semaphore` et `synchronized` ?** `synchronized` laisse entrer un seul thread, avec un propriétaire du verrou. `Semaphore` laisse entrer N threads et n'a pas de propriétaire : n'importe quel thread peut faire `release()`.
2. **Où doit-on appeler `release()` ?** Dans un `finally`, juste après un `acquire()` réussi.
3. **Différence entre `CountDownLatch` et `CyclicBarrier` ?** Latch : usage unique, des threads attendent que d'autres décomptent. Barrière : réutilisable, les mêmes threads s'attendent entre eux, avec une action de barrière.
4. **Que se passe-t-il si un thread n'appelle jamais `countDown()` ?** `await()` ne se termine jamais : d'où le `finally` et le timeout.
5. **Qu'est-ce qu'une `BrokenBarrierException` ?** Un thread de la barrière a été interrompu, a expiré ou a échoué : la barrière est cassée pour tous les autres.
6. **Quand utiliser un `Exchanger` ?** Échange de tampons entre deux threads (double buffering), avec un seul partenaire à la fois.
7. **Pourquoi `synchronized` garantit-il la visibilité ?** La sortie du bloc *publie* les écritures et l'entrée suivante les *voit* (relation happens-before sur le même verrou).
8. **Peut-on réutiliser un `CountDownLatch` ?** Non. Pour recommencer : un nouveau latch, ou une `CyclicBarrier`.

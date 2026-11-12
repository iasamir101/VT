# Structures de données thread-safe : fiches de révision

Package racine : `fr.vt.simulation.threadclassique.structuresdonnees` (les packages `etape1_atomicinteger` à `etape3_concurrenthashmap` ci-dessous en sont des sous-packages).

Fil conducteur : **comment modifier une donnée partagée sans perdre d'écriture**. Les deux premières étapes le font avec le **CAS** (Compare-And-Swap) sur une variable ; la troisième l'applique à une map entière.

| Étape | Package | Démo | Notion |
|---|---|---|---|
| 1 | `etape1_atomicinteger` | `AtomicIntegerDemo` | `AtomicInteger`, CAS sur un entier |
| 2 | `etape2_atomicreference` | `AtomicReferenceDemo` | `AtomicReference`, CAS sur un objet |
| 3 | `etape3_concurrenthashmap` | `ConcurrentHashMapDemo` | `HashMap` vs `ConcurrentHashMap` |

Les étapes sont numérotées dans l'ordre d'apprentissage conseillé :

| Ordre | Notion | Pourquoi à cette place |
|---|---|---|
| 1 | `AtomicInteger` | C'est le plus simple, et il rejoint la démo de race condition déjà écrite (`RaceConditionDemo`) : on corrige `counter++` sans `synchronized`. |
| 2 | `AtomicReference` | Il généralise le CAS à n'importe quel objet (`compareAndSet` dans une boucle de réessai). |
| 3 | `ConcurrentHashMap` | Son atomicité (`merge`, `compute`, `putIfAbsent`) devient plus facile à comprendre une fois le CAS vu. |

Lancer une démo (depuis `simulation/`) :

```bash
mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.structuresdonnees.<package>.<Demo>
```

Chaque fiche suit le même plan : notion, déroulement, avantages, limites, astuces, bonnes pratiques, à retenir.

---

## 1. `AtomicIntegerDemo` : un compteur sans verrou

### Notion
Un `AtomicInteger` est un entier dont les opérations (`incrementAndGet`, `addAndGet`, `compareAndSet`…) sont **atomiques** : elles ne peuvent pas être entrecoupées par un autre thread. Il s'appuie sur une instruction du processeur, le **Compare-And-Swap (CAS)** : « écrire la nouvelle valeur seulement si la valeur actuelle est celle que j'ai lue, sinon recommencer ».

### Déroulement
1. Deux threads font chacun **100 000** incréments sur deux compteurs partagés : un `int` ordinaire et un `AtomicInteger`.
2. Sur l'`int`, `counter++` fait trois étapes (lire, ajouter, écrire) : les threads se marchent dessus et des incréments sont perdus.
3. Sur l'`AtomicInteger`, `incrementAndGet()` est indivisible : aucun incrément perdu.
4. Après les `join()`, `main` compare : attendu `200000`, `int` environ **169 725** (change à chaque exécution), `AtomicInteger` toujours **200 000**.

### Avantages
- Pas de verrou : aucun thread n'est mis en attente ni endormi, donc pas de risque de **deadlock** avec une seule variable.
- Plus léger qu'un `synchronized` quand la contention est faible ou modérée.
- API riche : `getAndIncrement`, `addAndGet`, `updateAndGet(fn)`, `accumulateAndGet(x, fn)`.

### Limites
- **Une seule variable** : deux `AtomicInteger` ne se modifient pas ensemble de façon atomique. Pour garder un invariant sur plusieurs valeurs, il faut un verrou ou un objet immuable dans un `AtomicReference`.
- Sous forte contention, les réessais du CAS s'accumulent (les threads « tournent » pour rien).
- Atomique ne veut pas dire « cohérent » : `if (c.get() < 10) c.incrementAndGet()` reste un check-then-act (deux appels). Il faut `updateAndGet` ou `compareAndSet`.

### Astuces
- `incrementAndGet()` renvoie la valeur **après**, `getAndIncrement()` la valeur **avant**.
- `compareAndSet(attendu, nouveau)` renvoie `true` si le swap a eu lieu : base de tout algorithme sans verrou.
- Pour un compteur très disputé (statistiques, métriques), `LongAdder` est plus rapide qu'`AtomicLong` : il répartit les écritures sur plusieurs cellules.
- Variantes : `AtomicLong`, `AtomicBoolean`.

### Bonnes pratiques
- Compteur ou indicateur simple partagé : préférer un atomique à `synchronized`.
- Utiliser `updateAndGet` / `accumulateAndGet` plutôt que `get()` puis `set()` : l'opération reste atomique.
- Ne pas mélanger accès atomiques et accès ordinaires à la même donnée.

### À retenir
> `AtomicInteger` = un compteur **atomique sans verrou**, basé sur le **CAS**. Parfait pour **une** variable ; ne protège pas un invariant sur plusieurs variables.

---

## 2. `AtomicReferenceDemo` : le CAS sur un objet

### Notion
`AtomicReference<V>` applique le CAS à une **référence** : `compareAndSet(attendu, nouveau)` remplace la référence **seulement si elle est toujours celle qu'on a lue**. Combiné à des objets qu'on ne modifie jamais (on en fabrique une copie), il permet des structures sans verrou.

### Déroulement
**Partie 1 : une écriture ordinaire (le problème).**
1. Deux threads écrivent chacun une valeur (`"B"` et `"C"`) dans la **même variable** `String`, sans protection.
2. Il n'y a **pas de vérification** : la dernière écriture l'emporte. Le programme affiche `B` ou `C` selon l'ordre imprévisible des threads (race condition).

**Partie 2 : `compareAndSet` (la solution).**
1. Un `AtomicReference<String>` vaut `"EN_ATTENTE"`. Deux threads tentent chacun de le changer : le thread 1 vers `"VALIDE"`, le thread 2 vers `"REFUSE"`.
2. Chaque thread appelle `compareAndSet("EN_ATTENTE", nouvelleValeur)` : « change le statut **seulement s'il vaut toujours** `EN_ATTENTE` ».
3. Le premier thread qui passe réussit (`true`) et le statut change. L'autre trouve que le statut n'est plus `EN_ATTENTE` : son `compareAndSet` renvoie `false` et **ne change rien**.
4. Le statut final est `VALIDE` ou `REFUSE` : jamais un mélange, et **un seul thread a gagné**.

### Avantages
- Mise à jour atomique d'un **objet entier** (pas seulement un nombre), sans verrou.
- Aucun thread bloqué : en cas d'échec, il réessaie.
- Fonctionne bien avec des objets **immuables** : on publie une version complète et cohérente d'un coup.

### Limites
- **Compare par référence (`==`), pas avec `equals()`** : deux objets égaux mais distincts ne sont pas reconnus.
- **Problème ABA** : la valeur passe de A à B puis revient à A entre la lecture et le CAS ; le CAS réussit sans voir le changement. `AtomicStampedReference` (valeur + numéro de version) le résout.
- Copier tout l'objet à chaque modification coûte cher.
- Sous forte contention, beaucoup de réessais perdus.

### Astuces
- `updateAndGet(fn)` et `accumulateAndGet` font la boucle CAS à votre place : `list.updateAndGet(l -> ajouter(l, x))`. La fonction peut être appelée plusieurs fois : elle doit être **sans effet de bord**.
- `getAndSet(valeur)` : échange atomique (renvoie l'ancienne référence).
- `compareAndSet(null, x)` est la façon sans verrou de faire une **initialisation unique** ou de désigner un « propriétaire ».

### Bonnes pratiques
- Publier des objets **immuables** (ou jamais modifiés après publication) dans l'`AtomicReference`.
- La fonction de calcul doit être courte, pure et rejouable.
- La démo montre le `compareAndSet` seul ; la **boucle de réessai** (lire, calculer une nouvelle version, `compareAndSet`, recommencer si `false`) est le schéma décrit ci-dessus mais n'est pas codée dans la démo.
- En vrai code, préférer une structure prête à l'emploi (`CopyOnWriteArrayList`, `ConcurrentLinkedQueue`) à une boucle CAS écrite à la main.

### À retenir
> `AtomicReference` = **CAS sur un objet** : lire, calculer une nouvelle version, `compareAndSet`, **recommencer en cas d'échec**. Compare par **référence** ; attention au problème **ABA**.

---

## 3. `ConcurrentHashMapDemo` : une map partagée entre threads

### Notion
`HashMap` n'est **pas thread-safe**. Deux threads qui écrivent en même temps corrompent sa structure interne. `ConcurrentHashMap` est conçue pour des accès concurrents.

### Déroulement
1. Deux threads insèrent chacun 100 000 clés **différentes** (0 à 99 999 pour l'un, 100 000 à 199 999 pour l'autre).
2. On attend les deux avec `join()`, puis on lit `map.size()`.
3. Résultat sur la `HashMap` : **moins de 200 000** clés, et la valeur change à chaque exécution (exemple : 160 091). Parfois une exception.
4. Résultat sur la `ConcurrentHashMap` : toujours **200 000**.

### Pourquoi la `HashMap` perd des données alors que les clés sont distinctes
Les threads n'écrivent pas la même clé, mais ils partagent la **structure interne** (tableau de cases, chaînage, redimensionnement). Deux `put()` simultanés peuvent :
- viser la même case, et l'un écrase le chaînage de l'autre ;
- tomber pendant un **redimensionnement** : des entrées sont recopiées dans l'ancien tableau et disparaissent.

`ConcurrentHashMap` verrouille **par case** (et gère le redimensionnement à plusieurs threads) : aucune insertion perdue.

### Avantages
- Lectures sans verrou, écritures sur des cases différentes en parallèle : bien plus scalable qu'une map entièrement verrouillée.
- Opérations atomiques prêtes à l'emploi : `putIfAbsent`, `computeIfAbsent`, `compute`, `merge`.
- Les itérateurs ne lèvent pas `ConcurrentModificationException` (ils sont « faiblement cohérents »).

### Limites
- **Chaque appel est atomique, pas un enchaînement d'appels.** `get()` puis `put()` reste une race condition (check-then-act) : deux threads peuvent lire la même valeur et l'un écrase l'autre. Avec un compteur, on perd des incréments. La solution est `map.merge(clé, 1, Integer::sum)`, atomique. *(La démo actuelle ne montre que l'insertion de clés distinctes, pas ce cas.)*
- Pas de clé ni de valeur `null` (`NullPointerException`).
- `size()` et les itérations sont des **instantanés approximatifs** si la map change en même temps.
- Pas de verrou exposé pour verrouiller plusieurs opérations ensemble.

### Astuces
- Pour compter : `merge(clé, 1, Integer::sum)`. Pour un compteur très disputé, `LongAdder` en valeur : `map.computeIfAbsent(k, x -> new LongAdder()).increment()`.
- Pour initialiser paresseusement : `computeIfAbsent(clé, k -> créer())`.
- Pourquoi la démo marche : beaucoup d'opérations (100 000 par thread) et **aucun `println`** dans la boucle. Avec peu d'opérations, un thread finit avant que l'autre démarre. Un `println` est synchronisé et ralentit tellement les threads qu'ils ne se chevauchent presque plus.
- Une démo sans perte de données ne prouve pas que le code est correct : une race condition est probabiliste.

### Bonnes pratiques
- Toute map partagée entre threads en écriture : `ConcurrentHashMap`, pas `HashMap`.
- Jamais de `if (!map.containsKey(k)) map.put(k, v)` : utiliser `putIfAbsent` ou `computeIfAbsent`.
- Garder les fonctions passées à `compute*` / `merge` **courtes**, sans effet de bord et sans appel à la même map : elles s'exécutent sous verrou.
- `Collections.synchronizedMap` fonctionne, mais un seul verrou pour toute la map : moins performant.

### À retenir
> `HashMap` + threads = données perdues même avec des clés distinctes. `ConcurrentHashMap` protège **chaque appel**, pas la séquence d'appels : utiliser `merge` / `compute` / `putIfAbsent`.

---

## Comparatif : choisir la bonne structure

| Besoin | Outil |
|---|---|
| Compteur ou indicateur simple partagé | `AtomicInteger` / `AtomicLong` / `AtomicBoolean` |
| Compteur très disputé (métriques) | `LongAdder` |
| Remplacer atomiquement un objet entier | `AtomicReference` (objet immuable) |
| Initialiser une valeur une seule fois, sans verrou | `compareAndSet(null, valeur)` |
| Map partagée entre threads | `ConcurrentHashMap` |
| Mettre à jour atomiquement une entrée de map | `merge` / `compute` / `computeIfAbsent` / `putIfAbsent` |
| Protéger un invariant sur plusieurs variables | `synchronized` ou un verrou (voir `synchroniseurs`) |

## Questions d'entretien fréquentes

1. **Qu'est-ce que le CAS ?** Une instruction atomique du processeur : écrire la nouvelle valeur seulement si la valeur actuelle est celle attendue, sinon échouer (et réessayer).
2. **Pourquoi `counter++` n'est-il pas thread-safe ?** Trois étapes (lire, ajouter, écrire) : deux threads peuvent lire la même valeur et perdre un incrément.
3. **`AtomicInteger` ou `synchronized` pour un compteur ?** `AtomicInteger` : sans verrou, plus léger. `synchronized` si plusieurs variables doivent rester cohérentes entre elles.
4. **Qu'est-ce que le problème ABA ?** La valeur passe de A à B puis revient à A : le CAS ne voit pas le changement. Solution : `AtomicStampedReference`.
5. **`compareAndSet` compare-t-il avec `equals()` ?** Non, par **référence** (`==`).
6. **`ConcurrentHashMap` est thread-safe : `get` puis `put` est-il sûr ?** Non (check-then-act) : utiliser `merge`, `compute` ou `putIfAbsent`.
7. **Pourquoi `HashMap` perd des données avec des clés différentes ?** Structure interne partagée et redimensionnement concurrent.
8. **`ConcurrentHashMap` ou `Collections.synchronizedMap` ?** `ConcurrentHashMap` : verrouillage par case et lectures sans verrou, bien plus scalable qu'un verrou unique sur toute la map.

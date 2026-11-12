# Lecteurs / rédacteurs : fiches de révision

Package racine : `fr.vt.simulation.threadclassique.lecteurredacteur` (sous-packages `etape1_readwritelock` et `etape2_stampedlock`).

Le problème : une donnée partagée est **lue très souvent** et **modifiée rarement**. Avec un `synchronized` classique, deux lecteurs s'attendent alors que lire en même temps est sans danger. On veut donc autoriser **plusieurs lecteurs en parallèle**, mais **un seul rédacteur à la fois**, et jamais de lecture pendant une écriture.

| Étape | Package | Démo | Notion |
|---|---|---|---|
| 1 | `etape1_readwritelock` | `ReadWriteLockDemo` | `ReentrantReadWriteLock` : verrou de lecture partagé, verrou d'écriture exclusif |
| 2 | `etape2_stampedlock` | `StampedLockDemo` | `StampedLock` : lecture **optimiste** sans verrou, validée après coup |

Ordre d'apprentissage : **1 puis 2**. Le verrou lecture/écriture classique donne le principe ; le `StampedLock` l'optimise pour les cas où les écritures sont très rares.

Lancer une démo (depuis `simulation/`) :

```bash
mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.lecteurredacteur.<package>.<Demo>
```

Chaque fiche suit le même plan : notion, déroulement, avantages, limites, astuces, bonnes pratiques, à retenir.

---

## 1. `ReadWriteLockDemo` : le verrou lecture / écriture

### Notion
Un `ReentrantReadWriteLock` fournit **deux verrous liés** :

| Verrou | Qui peut le détenir en même temps |
|---|---|
| **Lecture** (`readLock()`) | **Plusieurs** lecteurs ensemble |
| **Écriture** (`writeLock()`) | **Un seul** rédacteur, et **aucun lecteur** pendant ce temps |

### Les classes du package

| Classe | Rôle |
|---|---|
| `ReadWriteLockDemo` | Point d'entrée : crée la donnée, 3 lecteurs et 2 rédacteurs, les démarre, `join()`, « Fin du traitement ». |
| `SharedData` | La donnée partagée (une chaîne, « Valeur initiale ») et le verrou. Toute la logique de verrouillage est dans `read()` et `write()`. |
| `Reader` | Thread qui appelle `read()` 3 fois. |
| `Writer` | Thread qui appelle `write("Nouvelle valeur i")` 3 fois. |

`Reader` et `Writer` ne connaissent pas le verrou : c'est `SharedData` qui le porte.

### Déroulement
1. `read()` prend `readLock().lock()`, affiche « <nom> lit : <valeur> », dort 1 s (lecture longue simulée), puis `unlock()` dans un `finally`.
2. `write()` prend `writeLock().lock()`, affiche « <nom> écrit : <valeur> », remplace la donnée, dort 1 s, puis `unlock()` dans un `finally`.
3. Le `sleep(1000)` sert à **rendre le chevauchement visible** : sans lui, les opérations seraient trop courtes pour qu'on les voie.
4. Les lecteurs affichent « lit » presque **en même temps** (lectures parallèles).
5. Les « écrit » arrivent **environ un par seconde** (écriture exclusive).
6. Un rédacteur attend la fin des lecteurs en cours ; les lecteurs attendent la fin du rédacteur. Aucune lecture n'a donc lieu pendant une écriture : on ne lit jamais une valeur en cours de modification.

### Avantages
- **Meilleur parallélisme** qu'un verrou unique quand les lectures dominent.
- Cohérence garantie : lectures et écritures ne se mélangent jamais.
- **Réentrant** : un thread qui détient déjà le verrou peut le reprendre.
- Modes équitable ou non (`new ReentrantReadWriteLock(true)`).

### Limites
- **Pas de montée de verrou** : un thread qui détient le verrou de lecture et demande celui d'écriture se **bloque lui-même** (deadlock). L'inverse est permis (rétrogradation : prendre le verrou de lecture avant de relâcher celui d'écriture).
- **Famine possible** : avec un flot continu de lecteurs, un rédacteur peut attendre longtemps. Le mode équitable règle cela, au prix du débit.
- **Plus coûteux qu'un verrou simple** : gérer deux verrous a un prix. Il n'est rentable que si les lectures sont **nombreuses**, **longues** et plus fréquentes que les écritures. Sinon `synchronized` est plus simple et aussi rapide.
- Les lecteurs doivent **vraiment ne pas modifier** la donnée : le verrou ne le vérifie pas.

### Astuces
- `tryLock()` / `tryLock(délai, unité)` : ne pas bloquer indéfiniment.
- `readLock().newCondition()` n'existe pas (non supporté), contrairement à `writeLock().newCondition()`.
- `getReadLockCount()`, `isWriteLocked()` : utiles pour déboguer.
- Pour des données immuables remplacées en bloc, une `volatile` ou un `AtomicReference` suffit souvent (voir `structuresdonnees`).

### Bonnes pratiques
- **`unlock()` toujours dans un `finally`** : un verrou explicite n'est pas libéré automatiquement. Sans `finally`, une exception laisse le verrou pris et bloque tous les autres threads.
- `lock()` juste **avant** le `try`, pas dedans.
- Garder les sections critiques courtes : pas d'I/O ni de `sleep` en vrai code (la démo en met un pour montrer le parallélisme).
- Encapsuler le verrou dans la classe qui protège la donnée (comme `SharedData`) : les appelants ne manipulent pas le verrou.

### À retenir
> `ReentrantReadWriteLock` : **plusieurs lecteurs OU un seul rédacteur**. `unlock()` dans un `finally`. Pas de montée de verrou lecture vers écriture. Rentable seulement si les lectures dominent.

---

## 2. `StampedLockDemo` : la lecture optimiste

### Notion
Un `StampedLock` ajoute un troisième mode : la **lecture optimiste**. On lit **sans prendre de verrou**, puis on vérifie après coup qu'aucune écriture n'a eu lieu. Chaque verrou renvoie un **stamp** (numéro de version) qu'il faut repasser pour le libérer ou le valider.

| Mode | Appel | Particularité |
|---|---|---|
| Lecture **optimiste** | `tryOptimisticRead()` + `validate(stamp)` | Aucun verrou, aucun blocage |
| Lecture classique | `readLock()` / `unlockRead(stamp)` | Verrou partagé |
| Écriture | `writeLock()` / `unlockWrite(stamp)` | Verrou exclusif |

### Les classes du package

| Classe | Rôle |
|---|---|
| `StampedLockDemo` | Point d'entrée : compte à 1 000, 3 lecteurs, 2 rédacteurs, puis « Solde final ». |
| `BankAccount` | Le compte partagé (`balance`) et le `StampedLock`. Trois méthodes : `getBalanceOptimistic()`, `getBalance()`, `deposit()`. |
| `BalanceReader` | Thread qui consulte le solde 5 fois en lecture optimiste. |
| `DepositWriter` | Thread qui dépose 100, 5 fois. |

### Déroulement
Dans `getBalanceOptimistic()` :
1. `tryOptimisticRead()` renvoie un stamp, **sans bloquer** personne.
2. On **copie** `balance` dans une variable locale.
3. `validate(stamp)` vérifie qu'aucun `writeLock` n'a été pris depuis l'étape 1.
4. Si c'est valide, on renvoie la copie : **aucun verrou n'a été pris**.
5. Sinon (une écriture est passée), la valeur lue est peut-être incohérente : on **relit sous un vrai `readLock()`**.

Dans `deposit()` : `writeLock()`, `balance += 100`, affichage « dépôt : 100.0 | Solde : … », `unlockWrite(stamp)`.

Résultat : les deux rédacteurs font 5 dépôts de 100 chacun, donc le solde final vaut toujours **1 000 + 2 × 5 × 100 = 2 000**. Un lecteur ne voit jamais une valeur incohérente, bien que la plupart du temps il ne prenne aucun verrou.

### Avantages
- **Plus rapide** que `ReentrantReadWriteLock` quand les écritures sont rares : les lecteurs ne se bloquent jamais entre eux ni les uns les autres.
- Les lecteurs ne retardent pas les rédacteurs (ils ne détiennent aucun verrou en mode optimiste), ce qui limite la famine des rédacteurs.
- Possibilité de convertir un verrou : `tryConvertToWriteLock(stamp)`.

### Limites
- **Non réentrant** : un thread qui reprend un verrou qu'il détient déjà se bloque lui-même.
- **Pas de `Condition`**.
- Plus délicat à écrire : il faut copier les valeurs lues dans des variables locales **avant** `validate()`, et prévoir le repli sous verrou.
- La lecture optimiste peut lire des données **incohérentes** avant la validation : le code entre la lecture et `validate()` ne doit rien faire de dangereux avec elles (pas de déréférencement, pas de division).
- Il faut **repasser le bon stamp** à `unlock` : se tromper lève une exception ou corrompt l'état.
- Peu utile si les écritures sont fréquentes : la validation échoue souvent et on relit sous verrou, donc on paye les deux.

### Astuces
- `tryOptimisticRead()` renvoie **0** si un verrou d'écriture est actuellement pris : `validate(0)` est toujours faux, ce cas est donc couvert par le même code.
- Le schéma à retenir : **lire en local, valider, sinon relire sous verrou**.
- `StampedLock` est sorti en Java 8 comme alternative plus performante, pas comme remplaçant systématique.

### Bonnes pratiques
- Copier **tous** les champs lus dans des variables locales, puis valider, puis seulement utiliser ces copies.
- `unlockRead` / `unlockWrite` dans un `finally`, avec le stamp **obtenu** (pas celui d'un autre appel).
- Ne l'utiliser que si on a **mesuré** un gain : en cas de doute, `ReentrantReadWriteLock` est plus simple à raisonner.
- Ne pas mélanger des lectures optimistes avec des verrous de lecture classiques sur la même donnée sans bien comprendre les stamps.

### À retenir
> `StampedLock` : lecture **optimiste** = lire sans verrou, **`validate()`**, et relire sous `readLock` en cas d'échec. Plus rapide si les écritures sont rares, mais **non réentrant** et plus délicat.

---

## Comparatif : `synchronized`, `ReentrantReadWriteLock`, `StampedLock`

| | `synchronized` | `ReentrantReadWriteLock` | `StampedLock` |
|---|---|---|---|
| Lecteurs en parallèle | non | oui | oui (et lecture sans verrou) |
| Réentrant | oui | oui | **non** |
| `Condition` | `wait` / `notify` | oui (écriture) | non |
| Montée lecture vers écriture | sans objet | impossible (deadlock) | possible (`tryConvertToWriteLock`) |
| Complexité d'usage | la plus simple | moyenne | la plus délicate (stamps, validation) |
| À choisir quand | peu de contention | lectures nombreuses et longues | lectures très nombreuses, écritures rares, performance mesurée |

## Questions d'entretien fréquentes

1. **À quoi sert un `ReadWriteLock` ?** Autoriser plusieurs lecteurs simultanés mais un seul rédacteur exclusif.
2. **Peut-on prendre le verrou d'écriture quand on détient celui de lecture ?** Non : le thread se bloque lui-même. L'inverse (rétrogradation) est permis.
3. **Pourquoi `unlock()` dans un `finally` ?** Un verrou explicite n'est pas libéré automatiquement : une exception le laisserait pris.
4. **Qu'est-ce que la famine des rédacteurs ?** Un flot continu de lecteurs empêche un rédacteur d'obtenir le verrou. Le mode équitable l'atténue.
5. **Qu'est-ce qu'une lecture optimiste ?** Lire sans verrou, puis vérifier avec `validate(stamp)` qu'aucune écriture n'a eu lieu ; sinon relire sous verrou.
6. **Pourquoi copier les champs dans des variables locales avant `validate()` ?** Les données lues avant la validation peuvent être incohérentes : on ne les utilise qu'après un `validate()` réussi.
7. **`StampedLock` est-il réentrant ?** Non, et il n'a pas de `Condition`.
8. **Quand préférer un simple `synchronized` ?** Peu de contention, ou écritures aussi fréquentes que les lectures : le verrou lecture/écriture ne rapporte alors rien.

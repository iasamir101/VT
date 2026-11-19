# mylib : réimplémenter les outils de concurrence

Package : `fr.vt.simulation.threadclassique.mylib`

Après avoir **utilisé** les outils de `java.util.concurrent`, on les **reconstruit** soi-même, en couches : chaque classe repose sur la précédente, jamais sur l'outil du JDK qu'elle remplace. C'est le meilleur moyen de comprendre CAS, attente et réveil, et c'est un exercice d'entretien classique.

| Étape | Classe de la bibliothèque | Repose sur | Remplace | Démo |
|---|---|---|---|---|
| 1 | `MyAtomicPairInteger` (entier toujours **pair**) | `volatile int` + méthodes `synchronized` | `AtomicInteger` | `etape1_atomicpair.AtomicPairDemo` |

**Mode de travail.** Les classes de la bibliothèque sont publiques, à la racine de `mylib`. Les méthodes à écrire contiennent un `TODO` avec des indices et lèvent `UnsupportedOperationException("TODO")` : le projet compile tout de suite. Chaque démo **se vérifie elle-même** et affiche `OK` ou `ÉCHEC : ...`.

```bash
mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.mylib.etape1_atomicpair.AtomicPairDemo
```

Lance chaque démo **plusieurs fois** : un bug de concurrence peut ne se montrer qu'une fois sur dix.

---

## Étape 1 : `MyAtomicPairInteger`

**Notion.** Un `AtomicInteger` maison qui ne contient que des nombres **pairs** : 0, 2, 4… Un incrément ajoute 2, un décrément retire 2, une valeur impaire est refusée (`IllegalArgumentException`). L'invariant « toujours pair » doit être vrai **à chaque lecture**, pas seulement à la fin.

**API.** `get()`, `compareAndSet(attendu, nouveau)`, `incrementAndGet()`, `getAndIncrement()`, `decrementAndGet()` et `addAndGet(delta)`.

**Schéma.** Un champ `volatile int value`. Les **écritures** (`compareAndSet`, `addAndGet`, `getAndIncrement`) sont des méthodes `synchronized` : « lire, calculer, écrire » se fait d'un bloc, un seul thread à la fois. La **lecture** `get()` se fait sans verrou : grâce à `volatile`, elle voit la dernière valeur écrite, et cette valeur est toujours paire, car elle est écrite en une seule fois. `incrementAndGet()` et `decrementAndGet()` délèguent à `addAndGet(±2)`.

```java
public synchronized int addAndGet(int delta) { requireEven(delta); value += delta; return value; }
public synchronized boolean compareAndSet(int expected, int newValue) {
    requireEven(newValue);
    if (value != expected) return false;   // comparer à la valeur COURANTE
    value = newValue; return true;
}
```

**Variante sans verrou (celle du JDK).** `AtomicInteger` n'utilise pas `synchronized`, mais une boucle CAS sur le champ via `VarHandle` (ou `Unsafe`) : `while (true) { c = value; if (CAS(c, c + 2)) return c + 2; }`. C'est plus rapide quand la contention est faible, et plus complexe à écrire.

**Pièges**
- `value += 2` sur un `volatile` n'est **pas** atomique : c'est lire, puis calculer, puis écrire. Deux threads peuvent lire la même valeur et un incrément est perdu. `volatile` = visibilité, pas atomicité.
- Deux opérations atomiques à la suite (`+1` puis `+1`) ne font pas une opération atomique : un lecteur peut voir la valeur impaire au milieu. La démo le montre avec `NaiveEvenCounter`.
- `compareAndSet` compare `expected` à la valeur **courante** (`value`), pas à `newValue`. Le test et l'écriture doivent être faits sous le même verrou ; sinon un autre thread peut modifier la valeur entre les deux.
- Vérifier la parité **avant** d'écrire (`compareAndSet(0, 5)` doit échouer sans rien modifier).
- `v % 2` vaut `-1` pour un impair négatif : tester `!= 0`, pas `== 1`.

**À retenir.** Un invariant se protège en faisant passer l'état d'une valeur valide à une autre valeur valide **en une seule opération atomique** (bloc `synchronized`, ou un seul CAS). Astuce classique : écritures sous verrou, lectures `volatile` sans verrou, ce qui suffit quand chaque écriture laisse un état valide.

---

## Questions d'entretien

1. *Pourquoi `volatile int` + `value += 2` ne suffit-il pas ? Et deux `incrementAndGet()` à la suite ?* `volatile` garantit la visibilité, pas l'atomicité de lire, calculer, écrire. Deux opérations atomiques à la suite ne forment pas une opération atomique. Il faut **un seul** CAS qui passe d'un état valide à un autre état valide.
2. *Comment `AtomicInteger.incrementAndGet()` est-il codé ?* Un champ `volatile int` et une boucle CAS (lire, calculer, `compareAndSet`, recommencer si raté), via `Unsafe` ou `VarHandle`. Sur x86, le CPU peut même le faire en une instruction (`lock xadd`).

## Suite prévue (plus tard)

Voir `PLAN-mylib.md` : `MyLocker` + `MyCondition` (verrou), `MySemaphore`, `MyConcurrentQueue` (file bloquante pour producteur / consommateur), puis `MyCountDownLatch`, `MyReadWriteLock`, `MyThreadPool`, `MyCyclicBarrier`.

# Deadlock : fiches de révision

Package racine : `fr.vt.simulation.threadclassique.deadlock` (les packages `etape1_deadlock` à `etape3_trylock` ci-dessous en sont des sous-packages).

Un **deadlock** (interblocage) survient quand des threads s'attendent mutuellement pour toujours. C'est le revers des verrous : la race condition (`RaceConditionDemo`) se corrige en verrouillant, mais dès qu'un thread prend **plusieurs** verrous, il risque le deadlock. Chaque fiche suit le même plan : notion, déroulement, avantages, limites, astuces, bonnes pratiques, à retenir.

| Étape | Package | Démo | Question à laquelle il répond |
|---|---|---|---|
| 1 | `etape1_deadlock` | `DeadlockDemo` | À quoi ressemble un deadlock, et comment le détecter ? |
| 2 | `etape2_lockordering` | `LockOrderingDemo` | Comment l'empêcher en prenant les verrous dans un ordre fixe ? |
| 3 | `etape3_trylock` | `TryLockDemo` | Comment l'empêcher quand on ne peut pas imposer d'ordre ? |

Lancer une démo (depuis `simulation/`) :

```bash
mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.deadlock.<package>.<Demo>
```

Fil conducteur : un **virement bancaire** entre deux comptes A et B. Un thread vire de A vers B (verrouille A puis B), l'autre de B vers A (verrouille B puis A). C'est le cas d'école des entretiens.

---

## Les 4 conditions de Coffman

Un deadlock n'est possible que si les **quatre** conditions sont réunies. Il suffit d'en casser **une** pour l'empêcher.

| Condition | Signification | Dans la démo | Cassée par |
|---|---|---|---|
| **Exclusion mutuelle** | Une ressource n'est détenue que par un thread à la fois. | `synchronized (compte)` | Rarement cassable (c'est le but du verrou). Alternative : pas de verrou (atomiques, objets immuables). |
| **Détention et attente** (*hold and wait*) | Un thread garde un verrou pendant qu'il en demande un autre. | Le thread garde A et attend B. | `TryLockDemo` : en cas d'échec, on **relâche** A. |
| **Pas de préemption** | On ne peut pas retirer un verrou à son détenteur. | Un `synchronized` n'est jamais repris de force. | `tryLock(délai)` : le thread **renonce** lui-même. |
| **Attente circulaire** | T1 attend T2, qui attend T1 (ou un cycle plus long). | A->B attend B, B->A attend A. | `LockOrderingDemo` : un **ordre global** rend tout cycle impossible. |

---

## 1. `DeadlockDemo` : le problème

### Notion
Deux threads verrouillent les mêmes ressources dans l'**ordre inverse**. Chacun obtient son premier verrou, puis attend le second, détenu par l'autre. Aucun ne relâche le sien : ils sont bloqués pour toujours, à l'état `BLOCKED`, sans consommer de CPU.

### Déroulement
1. Le thread « virement A->B » entre dans `synchronized (A)`, le thread « virement B->A » entre dans `synchronized (B)`.
2. Une pause de 100 ms entre les deux verrous garantit que chacun a pris le premier avant de demander le second.
3. A->B demande B (détenu par B->A), B->A demande A (détenu par A->B) : **attente circulaire**.
4. Après 1 s, `main` appelle `ThreadMXBean.findDeadlockedThreads()` et affiche, pour chaque thread, le verrou attendu et son détenteur.
5. Les soldes sont intacts (1 000 et 1 000) : aucun virement n'a abouti.
6. `main` termine par `System.exit(0)`, car les threads bloqués ne finiront jamais.

### Pourquoi la pause et les threads daemon
- Sans la pause, le premier thread prendrait souvent les deux verrous avant que le second démarre : le deadlock serait **possible mais rare**, ce qui est encore plus dangereux en production.
- Un thread bloqué sur un moniteur ne peut être ni interrompu ni arrêté. En daemon, il n'empêche pas la JVM de s'arrêter. Sous `exec:java`, on force quand même l'arrêt avec `System.exit`.

### Avantages (de la démo)
- Le deadlock se produit **à chaque exécution**.
- La détection par programme montre exactement le cycle : qui attend quoi, détenu par qui.

### Limites
- `findDeadlockedThreads()` **détecte** mais ne **corrige** pas : on ne peut pas débloquer un thread `synchronized`.
- Il ne voit que les cycles de **verrous** (moniteurs et `Lock`). Il ne voit pas un deadlock « logique » : deux threads qui attendent chacun un message de l'autre sur une `BlockingQueue`, ou un pool dont toutes les tâches attendent d'autres tâches du même pool.

### Astuces
- En dehors du programme : `jstack <pid>` ou `jcmd <pid> Thread.print` affichent « Found one Java-level deadlock » avec les deux piles. `jps` donne le `pid`. VisualVM et JConsole ont aussi un bouton de détection.
- Symptôme typique en production : application qui ne répond plus, CPU à **0 %**, threads `BLOCKED`. (Un CPU à 100 % évoque plutôt un livelock ou une boucle infinie.)
- Nommer ses threads (`new Thread(r, "virement A->B")`) rend les dumps lisibles.

### Bonnes pratiques
- Éviter de détenir **plusieurs verrous** à la fois. Si c'est nécessaire, fixer un ordre (étape 2).
- Ne pas appeler de **code inconnu** (callback, méthode redéfinissable, listener) en détenant un verrou : il peut prendre d'autres verrous.
- Garder les sections critiques courtes : pas d'I/O ni de `sleep` sous verrou en vrai code.

### À retenir
> Deadlock = chacun **détient** un verrou et **attend** celui de l'autre. Il faut les 4 conditions de Coffman. Symptôme : threads `BLOCKED`, CPU à 0 %. Diagnostic : `jstack` ou `ThreadMXBean.findDeadlockedThreads()`.

---

## 2. `LockOrderingDemo` : ordre global des verrous

### Notion
Si **tous** les threads prennent les verrous dans le **même ordre**, aucun cycle n'est possible : celui qui obtient le premier verrou est le seul à pouvoir demander le second. On casse la condition d'**attente circulaire**. Ici, l'ordre est donné par l'`id` du compte : le plus petit d'abord.

### Déroulement
1. Les deux comptes ont un `id` : A = 1, B = 2.
2. `transfer(from, to)` calcule `first` (le plus petit id) et `second`, quel que soit le sens du virement.
3. Les deux threads verrouillent donc toujours **A puis B**.
4. Chaque thread fait 10 000 virements de 1 dans son sens, en même temps que l'autre.
5. Résultat : `20000 virements terminés en 5 ms`, puis `A = 1000, B = 1000, total = 2000`. Le programme se termine toujours.

### Avantages
- Solution **simple** et **déterministe** : aucun échec, aucun retry.
- Garde `synchronized` : pas de `unlock()` à oublier.
- C'est la réponse attendue en entretien pour le virement bancaire.

### Limites
- Il faut un **critère d'ordre** stable et unique pour chaque ressource (id, numéro de compte). Avec `System.identityHashCode`, deux objets peuvent avoir le même hash : il faut alors un verrou de départage (*tie-breaker*).
- La règle doit être respectée **partout** dans le code : un seul endroit qui verrouille dans l'autre ordre suffit à réintroduire le deadlock.
- Ne marche pas quand on ne connaît pas à l'avance tous les verrous nécessaires.

### Astuces
- Centraliser la prise des verrous dans **une seule méthode** (`transfer`) pour que l'ordre ne puisse pas être oublié.
- Pour N ressources : les trier par id, puis les verrouiller dans cet ordre.

### Bonnes pratiques
- Documenter l'ordre des verrous (commentaire, Javadoc) : c'est une règle d'architecture, pas un détail.
- Préférer, quand c'est possible, **un seul verrou** plus large ou une structure sans verrou.

### À retenir
> Toujours prendre les verrous **dans le même ordre global** (par exemple par id croissant) : l'attente circulaire devient impossible, donc le deadlock aussi.

---

## 3. `TryLockDemo` : renoncer avec `tryLock(délai)`

### Notion
`ReentrantLock.tryLock(délai)` attend un verrou **au plus** pendant le délai, puis renvoie `false` au lieu de bloquer pour toujours. Si le second verrou n'arrive pas, le thread **relâche** le premier et réessaie plus tard. On casse les conditions **détention et attente** et **pas de préemption**, même si les verrous sont pris dans l'ordre inverse.

### Déroulement
1. Chaque compte porte son propre `ReentrantLock`.
2. A->B fait `A.tryLock(50 ms)`, puis (après une pause de 20 ms qui force le croisement) `B.tryLock(50 ms)`. B->A fait l'inverse.
3. Le croisement se produit : les deux échouent sur leur second verrou, affichent « échec… je relâche… », et rendent le premier dans le `finally`.
4. Chacun attend un délai **aléatoire** (10 à 60 ms), puis recommence : l'un des deux finit par prendre les deux verrous.
5. Résultat : les 10 virements aboutissent (par exemple en 20 tentatives), total = 2000.

### Pourquoi un délai aléatoire
Si les deux threads échouaient puis réessayaient **au même instant**, ils pourraient échouer encore et encore : c'est un **livelock** (ils travaillent mais n'avancent pas). Le délai aléatoire (*backoff*) les désynchronise.

### Avantages
- Fonctionne **sans ordre global**, par exemple quand les verrous sont connus au fur et à mesure.
- Le thread garde la main : il peut abandonner, journaliser, renvoyer une erreur.
- `tryLock(délai)` est **interruptible** (il lève `InterruptedException`).

### Limites
- Plus de code : deux `try/finally`, une boucle de retry, une gestion d'échec.
- Des **échecs** et des tentatives perdues : moins efficace que l'ordre global quand celui-ci est possible.
- Risque de **livelock** sans délai aléatoire, et de **famine** si un thread perd toujours.
- `tryLock()` **sans** délai ignore l'équité : il prend le verrou s'il est libre, même si d'autres attendent.

### Astuces
- Limiter le nombre de tentatives, puis signaler l'échec (exception, message) plutôt que de boucler sans fin.
- `lockInterruptibly()` est une autre option : un thread bloqué peut alors être débloqué par `interrupt()`, ce qui est impossible avec `synchronized`.

### Bonnes pratiques
- `unlock()` **toujours dans un `finally`**, un `finally` par verrou obtenu.
- Ne jamais appeler `unlock()` sur un verrou qu'on n'a pas obtenu (`IllegalMonitorStateException`).
- Préférer l'ordre global (étape 2) quand il est possible ; garder `tryLock` pour les cas où il ne l'est pas.

### À retenir
> `tryLock(délai)` : on **renonce** au lieu d'attendre pour toujours, on **relâche** ce qu'on détient, on attend un délai **aléatoire**, on réessaie. Le deadlock ne peut pas durer ; le délai aléatoire évite le livelock.

---

## Deadlock, livelock, famine

| Problème | Les threads… | CPU | Exemple | Remède |
|---|---|---|---|---|
| **Deadlock** | sont **bloqués** pour toujours | ~0 % | Virements croisés (`DeadlockDemo`) | Ordre global, `tryLock(délai)` |
| **Livelock** | **travaillent** sans avancer | élevé | Deux `tryLock` qui échouent et réessaient en même temps | Délai aléatoire (*backoff*) |
| **Famine** (*starvation*) | avancent, sauf un qui n'obtient **jamais** la ressource | variable | Un rédacteur face à un flot continu de lecteurs | Verrou équitable (`new ReentrantLock(true)`), priorités revues |

## Comparatif : quelle prévention choisir

| Technique | Condition cassée | Quand l'utiliser |
|---|---|---|
| Un seul verrou | Détention et attente | Le plus simple, si la contention reste faible |
| Ordre global (`LockOrderingDemo`) | Attente circulaire | Les verrous nécessaires sont connus et ordonnables |
| `tryLock(délai)` + retry (`TryLockDemo`) | Détention et attente, pas de préemption | Pas d'ordre possible, ou besoin d'abandonner proprement |
| Pas de verrou (atomiques, immuables, structures concurrentes) | Exclusion mutuelle | Quand une structure de la bibliothèque fait le travail |

---

## Questions d'entretien fréquentes

1. **Qu'est-ce qu'un deadlock ?**
   Des threads qui s'attendent mutuellement pour toujours : chacun détient un verrou et attend celui d'un autre. Ils restent `BLOCKED` sans consommer de CPU. (`DeadlockDemo`)

2. **Quelles sont les conditions nécessaires ?**
   Les 4 conditions de Coffman : exclusion mutuelle, détention et attente, pas de préemption, attente circulaire. Il suffit d'en casser une.

3. **Écrivez un virement bancaire sans deadlock.**
   Verrouiller les deux comptes dans un ordre global, par exemple par id croissant, quel que soit le sens du virement. (`LockOrderingDemo`)

4. **Comment détecter un deadlock en production ?**
   `jstack <pid>` ou `jcmd <pid> Thread.print` (« Found one Java-level deadlock »), VisualVM ou JConsole, ou par programme `ThreadMXBean.findDeadlockedThreads()`. Symptôme : application figée, CPU à 0 %.

5. **Peut-on débloquer un thread en deadlock sur `synchronized` ?**
   Non : il n'est pas interruptible. Il faut redémarrer. Avec `ReentrantLock`, `lockInterruptibly()` ou `tryLock(délai)` permettent d'en sortir. (`TryLockDemo`)

6. **Qu'est-ce qu'un livelock ?**
   Les threads ne sont pas bloqués, ils réagissent sans cesse les uns aux autres sans avancer (deux personnes qui s'écartent du même côté dans un couloir). Remède : un délai aléatoire avant de réessayer.

7. **Un deadlock peut-il arriver avec un seul verrou ?**
   Pas avec un `synchronized` ou un `ReentrantLock`, car ils sont **réentrants** : le même thread peut les reprendre. Mais un verrou non réentrant, ou un `Semaphore(1)` pris deux fois par le même thread, se bloque lui-même. Et un deadlock peut arriver **sans verrou** : un pool à 1 thread dont la tâche attend (`future.get()`) une autre tâche soumise au même pool.

8. **Comment éviter les deadlocks dans un code qu'on ne maîtrise pas entièrement ?**
   Ne pas appeler de code étranger (callbacks, listeners) en détenant un verrou ; réduire la portée et le nombre de verrous ; préférer les structures concurrentes de la bibliothèque.

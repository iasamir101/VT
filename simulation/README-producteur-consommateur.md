# Producteur / consommateur : fiches de révision

Package racine : `fr.vt.simulation.threadclassique.producteurconsommateur` (sous-packages `etape1_waitnotify` et `etape2_blockingqueue`).

Le problème est le même dans les deux démos : un **producteur** dépose des éléments dans un tampon partagé, un **consommateur** les retire, à des vitesses différentes. Il faut que le producteur attende quand le tampon est plein, que le consommateur attende quand il est vide, et qu'aucun élément ne soit perdu ni lu deux fois.

| Étape | Package | Démo | Notion |
|---|---|---|---|
| 1 | `etape1_waitnotify` | `WaitNotifyProducerConsumerDemo` | Tampon borné écrit à la main : `synchronized`, `wait()`, `notifyAll()` |
| 2 | `etape2_blockingqueue` | `BlockingQueueProducerConsumerDemo` | `BlockingQueue` : la file gère seule l'attente, et une *poison pill* signale la fin |

Lancer une démo (depuis `simulation/`) :

```bash
mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.producteurconsommateur.<package>.<Demo>
```

Ordre d'apprentissage : **1 puis 2**. On écrit d'abord le mécanisme à la main pour comprendre ce que fait la file bloquante, puis on utilise la solution standard.

Chaque fiche suit le même plan : notion, déroulement, avantages, limites, astuces, bonnes pratiques, à retenir.

---

## 1. `WaitNotifyProducerConsumerDemo` : le tampon écrit à la main

### Notion
Un tampon borné (`BoundedBuffer`) protégé par le **moniteur** de l'objet. Les threads se coordonnent avec trois méthodes de `Object` :
- `wait()` : le thread **relâche le verrou** et s'endort jusqu'à une notification ;
- `notify()` / `notifyAll()` : réveille un ou tous les threads qui attendent sur cet objet.

Elles ne s'appellent que dans un bloc ou une méthode `synchronized` sur le même objet (sinon `IllegalMonitorStateException`).

### Déroulement
1. Un tampon de **5 places**. Un producteur dépose les entiers 1 à 10 (un toutes les **500 ms**), un consommateur les lit (un toutes les **1 000 ms**).
2. `produce(valeur)` : tant que le tampon est plein, le producteur affiche « Buffer plein : producteur en attente » et fait `wait()`. Sinon il ajoute la valeur et appelle `notifyAll()`.
3. `consume()` : tant que le tampon est vide, le consommateur affiche « Buffer vide : consommateur en attente » et fait `wait()`. Sinon il retire la valeur la plus ancienne et appelle `notifyAll()`.
4. Le consommateur étant deux fois plus lent, les « Produit » prennent de l'avance, le tampon se remplit, puis le producteur est freiné.
5. `main` attend les deux threads avec `join()`, puis affiche « Application terminée ». Les 10 valeurs sont consommées **dans l'ordre**, aucune n'est perdue.

### Pourquoi `while` et pas `if`
Au réveil, la condition peut être **redevenue fausse** : un autre thread est passé avant, ou le réveil est **intempestif** (*spurious wakeup*, permis par la spécification Java). Avec `if`, le thread continuerait à tort. Avec `while`, il retest la condition à chaque réveil.

### Pourquoi `notifyAll()` et pas `notify()`
Producteur et consommateurs attendent sur **le même objet** pour des raisons différentes. `notify()` réveille un seul thread au hasard : ce peut être le mauvais (un consommateur qui attend alors que le tampon est toujours vide), et le programme se bloque. `notifyAll()` réveille tout le monde ; chacun retest sa condition et les autres se rendorment.

### Avantages
- Montre le mécanisme interne : indispensable pour comprendre ce que cachent les files bloquantes.
- Aucune dépendance : seulement `synchronized`, `wait` et `notifyAll`.
- Contrôle total sur la condition d'attente.

### Limites
- **Verbeux et fragile** : une condition oubliée, un `if` au lieu de `while`, ou un `notify()` au lieu de `notifyAll()` donnent des bugs rares et difficiles à reproduire.
- **Une seule file d'attente par objet** : producteurs et consommateurs attendent sur le même moniteur et se réveillent mutuellement pour rien (avec `Lock` et `Condition`, on peut avoir une condition « plein » et une condition « vide »).
- Pas de timeout ni de `tryLock` : `wait()` peut durer indéfiniment si la notification n'arrive jamais.
- Pas d'arrêt propre prévu : la démo s'arrête car le nombre d'éléments (10) est connu à l'avance.

### Astuces
- `wait(délai)` limite l'attente.
- La boucle `while (condition) wait();` est un **schéma à connaître par cœur** : tester, attendre, retester.
- `wait()` lève `InterruptedException` : la remonter ou restaurer le statut d'interruption avec `Thread.currentThread().interrupt()`.
- Ne pas faire de `sleep` dans une section `synchronized` : il ne libère pas le verrou (dans la démo, le `sleep` est **dans le thread**, après `produce()` / `consume()`, donc hors verrou).

### Bonnes pratiques
- Toujours `wait()` dans une boucle `while` qui teste la condition.
- Modifier l'état (la file) et notifier **dans le même bloc synchronisé**.
- Préférer `notifyAll()` sauf si on sait exactement pourquoi `notify()` suffit.
- En vrai code, ne pas réécrire ce mécanisme : utiliser une `BlockingQueue` (étape 2).

### À retenir
> `wait()` **relâche le verrou** et s'endort ; `notifyAll()` réveille ; toujours dans un **`while`**. Écrire ce schéma à la main sert à comprendre, pas à produire du code.

---

## 2. `BlockingQueueProducerConsumerDemo` : la solution standard

### Notion
Une `BlockingQueue` est une file thread-safe qui **bloque** :
- `put(x)` bloque si la file est **pleine** (contre-pression) ;
- `take()` bloque si la file est **vide**.

Ici une `ArrayBlockingQueue` de capacité 5. Plus aucun `wait` ni `notify` dans le code : la file gère seule l'attente. Pour signaler la fin, le producteur envoie une valeur spéciale, la **poison pill** (`-1`).

### Déroulement
1. Un producteur (`Producer`, un `Thread`) envoie les entiers 1 à 10 (un toutes les **500 ms**) dans la file, puis la poison pill `-1`.
2. Un consommateur (`Consumer`, un `Thread`) fait `take()` en boucle et traite un élément toutes les **1 000 ms**.
3. Le consommateur étant deux fois plus lent, la file se remplit (5 éléments) : `put()` **freine** le producteur. C'est la contre-pression.
4. Le producteur affiche « Producteur : terminé » après avoir envoyé `-1`.
5. Le consommateur reçoit `-1`, sort de sa boucle et affiche « Consommateur : terminé », **sans avoir été interrompu**.
6. `main` fait les `join()` et affiche « Application terminée ».

### Avantages
- **Très court et lisible** : tout le travail de synchronisation est dans la file.
- Contre-pression automatique avec une file **bornée** : un producteur trop rapide ne fait pas exploser la mémoire.
- Plusieurs producteurs et plusieurs consommateurs sans changer le code de la file.
- Arrêt propre grâce à la poison pill : aucune interruption nécessaire.

### Limites
- **Une poison pill par consommateur** : avec 3 consommateurs, il faut en envoyer 3 ; sinon certains restent bloqués sur `take()`.
- La valeur spéciale doit être **impossible** comme donnée réelle (ici `-1`). Avec des données quelconques, utiliser un objet marqueur dédié.
- Si le producteur plante avant d'envoyer la poison pill, le consommateur reste bloqué indéfiniment.
- Une file bornée peut bloquer le producteur longtemps si le consommateur est en panne.

### Astuces
- **Choix de la file** : `ArrayBlockingQueue` (bornée, tableau), `LinkedBlockingQueue` (bornée ou non), `PriorityBlockingQueue` (par priorité, non bornée), `SynchronousQueue` (aucun stockage : chaque `put` attend un `take`), `DelayQueue` (éléments disponibles après un délai).
- Les variantes sans blocage ou avec délai : `offer(x)`, `offer(x, délai, unité)`, `poll()`, `poll(délai, unité)`. Utiles pour ne pas attendre indéfiniment.
- Alternative à la poison pill : un indicateur `volatile boolean` + `poll(délai)`, ou l'interruption du thread.

### Bonnes pratiques
- **Borner la file** (capacité fixe) pour avoir de la contre-pression.
- Toujours prévoir comment les consommateurs s'arrêtent (poison pill, interruption, timeout).
- Envoyer autant de poison pills que de consommateurs.
- Privilégier une `BlockingQueue` à un `wait/notify` écrit à la main.

### À retenir
> `BlockingQueue` : `put()` bloque si **pleine**, `take()` bloque si **vide**. File **bornée** = contre-pression. Fin propre avec une **poison pill** (une par consommateur).

---

## Comparatif : `wait/notify` ou `BlockingQueue`

| | `wait` / `notifyAll` (étape 1) | `BlockingQueue` (étape 2) |
|---|---|---|
| Code à écrire | tampon, verrou, conditions, boucle `while` | quelques lignes : `put` / `take` |
| Risque de bug | élevé (`if` au lieu de `while`, `notify`, oubli de notification) | faible |
| Contre-pression | à coder (taille max du tampon) | native (file bornée) |
| Timeout | difficile | `offer(délai)`, `poll(délai)` |
| Arrêt propre | à coder | poison pill ou interruption |
| Quand l'utiliser | pour comprendre, ou cas très particulier | dans presque tous les cas |

## Questions d'entretien fréquentes

1. **Que fait `wait()` du verrou ?** Il le **relâche** et endort le thread jusqu'à une notification. À la sortie de `wait()`, le thread reprend le verrou.
2. **Pourquoi mettre `wait()` dans un `while` ?** La condition peut être redevenue fausse au réveil (autre thread passé avant, ou réveil intempestif).
3. **`notify()` ou `notifyAll()` ?** `notifyAll()` par défaut : avec `notify()`, on peut réveiller le mauvais thread et bloquer le programme.
4. **Pourquoi `wait()` doit-il être dans un bloc `synchronized` ?** Sinon `IllegalMonitorStateException` : on doit détenir le moniteur de l'objet.
5. **Différence entre `sleep()` et `wait()` ?** `sleep()` ne relâche aucun verrou et se réveille tout seul ; `wait()` relâche le verrou et attend une notification.
6. **À quoi sert la contre-pression ?** Freiner le producteur quand le consommateur ne suit pas, avec une file **bornée**, pour éviter l'accumulation en mémoire.
7. **Qu'est-ce qu'une poison pill ?** Une valeur spéciale envoyée dans la file pour dire au consommateur de s'arrêter. Il en faut une par consommateur.
8. **Quelle `BlockingQueue` choisir ?** `ArrayBlockingQueue` / `LinkedBlockingQueue` pour une file bornée classique ; `SynchronousQueue` pour un transfert direct sans stockage ; `PriorityBlockingQueue` pour un ordre par priorité.

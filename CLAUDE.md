# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project purpose

"VT" is a Java learning project on concurrency, aimed at mastering the topics and preparing for technical interviews. The scope is defined in `Objectif.txt` (written in French) and has two parts:

**Multithreading (classic)**
- Thread / process
- Race conditions / deadlocks
- Synchronization: `synchronized`, `Semaphore`, atomics, `CyclicBarrier`
- Thread pools and `ExecutorService`
- Thread-safe collections
- Producer / consumer
- Readers / writers

**Virtual threads**
- Core principles
- Creation APIs
- Structured Concurrency
- `ScopedValue` vs `ThreadLocal`
- Anti-patterns to know
- Monitoring
- Interview concepts

## Repository layout

The folder is a git repository (`origin` = https://github.com/iasamir101/VT.git, branch `master`). Main content:

- `simulation/`: Maven project (Java 21, no dependencies) holding the runnable examples, package root `fr.vt.simulation`:
  - `threadclassique`: classic multithreading, one sub-package per notion; learning-order sub-packages are named `etapeN_name`, numbered from 1 (`etape0_race` is the one exception: step 0 of synchronisation, the problem the other steps solve) (`synchroniseurs.{etape0_race (race condition),etape1_semaphore,etape2_synchronized,etape3_countdownlatch,etape4_cyclicbarrier,etape5_exchanger}`, `deadlock.{etape1_deadlock,etape2_lockordering,etape3_trylock}` (the problem with detection, then fix by lock ordering, then fix by `tryLock` timeout; bank-transfer theme), `executor.{etape1_pool,etape2_callablefuture,etape3_completablefuture}` (numbered by learning order: pool, then Future, then CompletableFuture), `structuresdonnees.{etape1_atomicinteger,etape2_atomiclong,etape3_atomicreference,etape4_concurrenthashmap}` (numbered by learning order: atomic CAS counter, `AtomicLong` vs `LongAdder` under contention, CAS on references, then thread-safe map), `producteurconsommateur.{etape1_waitnotify,etape2_blockingqueue}`, `lecteurredacteur.{etape1_readwritelock,etape2_stampedlock}`).
  - `virtualthread`: virtual-thread examples (not created yet; add the package with the first example).
  - Each example has one entry point named `*Demo` with a `main`. Every class starts with a Javadoc header in French (But / Entrée / Sortie, and for `*Demo` classes an expected-output sample, what to observe, and the `exec:java` command); keep that header format for new examples; helper classes are package-private in the same sub-package, which is why sub-packages can reuse names like `Producer`, `Consumer`, `Reader`.
  - Not covered by an example yet: all virtual-thread topics.
- French study sheets (notion, flow, pros/cons, tips, best practices, takeaways), one README per topic with a meaningful name; add a section to the matching README when a new example is added:
  - `simulation/README-pools-et-futures.md`: `executor.*` (`FixedThreadPoolDemo`, `CallableFutureDemo`, `CompletableFuture*Demo`).
  - `simulation/README-structures-de-donnees.md`: `structuresdonnees.*` (`AtomicIntegerDemo`, `AtomicLongDemo`, `AtomicReferenceDemo`, `ConcurrentHashMapDemo`).
  - `simulation/README-synchroniseurs.md`: `synchroniseurs.*` (`RaceConditionDemo` as step 0, `SemaphoreParkingDemo`, `SynchronizedCounterDemo`, `CountDownLatchDemo`, `CyclicBarrierDemo`, `ExchangerDemo`).
  - `simulation/README-deadlock.md`: `deadlock.*` (`DeadlockDemo`, `LockOrderingDemo`, `TryLockDemo`), plus Coffman conditions and deadlock / livelock / starvation.
  - `simulation/README-producteur-consommateur.md`: `producteurconsommateur.*` (`WaitNotifyProducerConsumerDemo`, `BlockingQueueProducerConsumerDemo`).
  - `simulation/README-lecteur-redacteur.md`: `lecteurredacteur.*` (`ReadWriteLockDemo`, `StampedLockDemo`).
  - `simulation/README-thread-classique.md`: global overview of the six READMEs above (deadlock is part 2, right after synchronisation) (learning order, per-topic summary, tool-choice table, interview questions); it treats `RaceConditionDemo` as step 0 of synchronisation. Update it when a README or package changes.
- `knowledge.md`: French refresher course covering every notion of `Objectif.txt`.
- `qcm/qcm.html`: the level-0 quiz (see below).

Conventions for examples: example files the author hands over belong in `threadclassique` unless stated otherwise. Classes and identifiers in English, comments and console output in French; comment the concurrency mechanism, not trivial code.

## Commands

Run from `simulation/`:

```bash
mvn compile                                                      # build
mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.synchroniseurs.etape0_race.RaceConditionDemo
```

There is no test suite. Examples are demos verified by running them and reading the console output (expected results are stated in each `*Demo` Javadoc).

## Level-0 quiz

- `qcm/qcm.html`: a 20-question multiple-choice quiz (12 multithreading, 8 virtual threads), published as the Artifact https://claude.ai/artifact/7WMEkMV516Yy1cbTaD6UBN. Questions live in the `QUESTIONS` array (theme, text, correct answer, three wrong ones). Question order and answer order are reshuffled on every attempt, and "Je ne sais pas" is always the last option.
- The page never reveals correct answers; it only shows the final score in %, the duration and the date.
- Each attempt is stored in the Artifact's database, collection `passages` (`date`, `dureeSec`, `pourcentage`, `bonnes`, `total`, `jeNeSaisPas`, `multithreading`, `virtualThreads`). Read it with the `ArtifactData` tool. Opened as a local file, the page falls back to the browser's localStorage.
- To change the quiz, edit `qcm/qcm.html` and republish it to the same URL so the history is kept.

## Conventions

- The author writes in French; keep explanations and comments in French unless asked otherwise.
- Virtual threads, Structured Concurrency and `ScopedValue` need a recent JDK (virtual threads are final from Java 21; Structured Concurrency and `ScopedValue` were preview APIs for several releases), so check the JDK version and whether `--enable-preview` is needed before writing examples that use them.
- Follow the topic order in `Objectif.txt` when adding material, so that each concept builds on the previous one.

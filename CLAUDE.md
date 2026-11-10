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
  - `threadclassique`: classic multithreading, one sub-package per notion (`race`, `synchronisation`, `executor`, `collections`, `producteurconsommateur.{waitnotify,blockingqueue}`, `lecteurredacteur.{readwritelock,stampedlock}`).
  - `virtualthread`: virtual-thread examples (empty for now, only `package-info.java`).
  - Each example has one entry point named `*Demo` with a `main`. Every class starts with a Javadoc header in French (But / Entrée / Sortie, and for `*Demo` classes an expected-output sample, what to observe, and the `exec:java` command); keep that header format for new examples; helper classes are package-private in the same sub-package, which is why sub-packages can reuse names like `Producer`, `Consumer`, `Reader`.
  - Not covered by an example yet: deadlock, atomics, `CyclicBarrier`, and all virtual-thread topics.
- `knowledge.md`: French refresher course covering every notion of `Objectif.txt`.
- `qcm/qcm.html`: the level-0 quiz (see below).

Conventions for examples: example files the author hands over belong in `threadclassique` unless stated otherwise. Classes and identifiers in English, comments and console output in French; comment the concurrency mechanism, not trivial code.

## Commands

Run from `simulation/`:

```bash
mvn compile                                                      # build
mvn -q compile exec:java -Dexec.mainClass=fr.vt.simulation.threadclassique.race.RaceConditionDemo
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

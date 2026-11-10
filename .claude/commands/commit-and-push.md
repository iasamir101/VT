---
description: Ajoute tout, commite, puis pousse sur origin
allowed-tools: Bash(git add:*), Bash(git status:*), Bash(git diff:*), Bash(git log:*), Bash(git commit:*), Bash(git push:*), Bash(git branch:*)
---

1. Exécute `git add -A`, puis `git status --short` et `git diff --cached` pour voir ce qui sera commité.
2. S'il n'y a rien à commiter, dis-le et arrête-toi.
3. Rédige un message de commit court (une ligne, en français, dans le style de `git log --oneline -5`).
4. Crée le commit avec `git commit`, en terminant le message par la ligne d'attribution indiquée dans le contexte de la session (Co-Authored-By), si elle est présente.
5. Pousse avec `git push` (jamais en `--force`). Si la branche n'a pas d'upstream, utilise `git push -u origin <branche>`.
6. Affiche le hash, le message du commit et le résultat du push. En cas d'échec du push, explique la cause sans réessayer en force.

Arguments éventuels (message imposé) : $ARGUMENTS

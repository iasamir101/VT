---
description: Ajoute tout au staging et crée un commit avec un message généré
allowed-tools: Bash(git add:*), Bash(git status:*), Bash(git diff:*), Bash(git log:*), Bash(git commit:*)
---

1. Exécute `git add -A`, puis `git status --short` et `git diff --cached` pour voir ce qui sera commité.
2. S'il n'y a rien à commiter, dis-le et arrête-toi.
3. Rédige un message de commit court (une ligne, en français, dans le style de `git log --oneline -5`), qui décrit le changement.
4. Crée le commit avec `git commit`, en terminant le message par la ligne d'attribution indiquée dans le contexte de la session (Co-Authored-By), si elle est présente.
5. Ne pousse pas. Affiche le hash et le message du commit.

Arguments éventuels (message imposé) : $ARGUMENTS

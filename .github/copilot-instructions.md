---
name: Base Instructions
description: This file contains base instructions like coding conventions and guidelines.
applyTo: "**"
---

# Base Instructions

Language: Java 21  
Application Type: #REPLACE_SUMMARY
Minecraft Version: 1.21.1  
Modding Framework: NeoForge v21.1

# Conventions

- Don't give up on a problem and suggest adding a `// TODO: fix` comment. Realize dead ends and think about solutions or alternatives. Interject with questions if needed and speak up when there's an objectively better path.
- Don't add comments for the sake of comments. Code should be self-explanatory and comments reserved for explanations or important notes.
- Try to use datapack JSONs before writing any Java code.
- Use 4 spaces for indentation.
- Add `//#region` indicators for logical code sections (without `#endregion`).
- Respect the existing code style and don't use javax annotations.
- In the output, instead of including unmodified members, only show the new or modified code and make use of comments like `/* existing code */`.

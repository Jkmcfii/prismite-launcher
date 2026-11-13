# Prismite

Prismite is a local-only JavaFX Minecraft mod manager.

What it does:
- lets you create isolated local instances
- imports an existing Minecraft folder
- lists the mods in the selected instance
- adds local `.jar` and `.zip` mods
- removes mods
- backs up and restores mod ZIP archives
- searches Modrinth and installs compatible results for the selected Minecraft version and loader

What it does not do:
- no sign-in
- no Prismite account
- no Google/Discord/Microsoft auth
- no fake OAuth flow
- no Minecraft account verification
- no launch bypass

Requirements:
- Java 17+
- Maven

Run:

```bash
cd prismite-client
mvn javafx:run
```

Windows users can run the same command in PowerShell. This project is local-only and does not replace the official Minecraft launcher or require Microsoft login.

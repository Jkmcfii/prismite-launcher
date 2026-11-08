# Prismite

Prismite is a local-only JavaFX Minecraft mod manager with a Modrinth-style discovery flow. It automatically creates a local starter instance on first run, searches the public Modrinth API, filters discovery by the selected Minecraft version and loader, installs compatible mod files and required Modrinth dependencies, and manages local mod folders/backups.

## Run

Requirements: Java 17+ and Maven.

```bash
cd prismite-client
mvn javafx:run
```

No Prismite, Google, Discord, or Microsoft account is required. Internet access is used only for the public Modrinth API and downloads. Minecraft sign-in and game launch remain with the official Minecraft launcher.

## Local data

- Instance metadata: `~/.prismite/instances.json`
- Isolated instance folders: `~/.prismite/instances/`
- Imported folders are not copied; Prismite manages the exact directory the user selects.

## Features

- Automatically prepares a starter instance at first run.
- Create isolated instances or import an existing game directory.
- Search Modrinth and view project descriptions.
- Filter search by selected Minecraft version and loader.
- Install a compatible release and recursively install required Modrinth project dependencies.
- Add/remove local `.jar` and `.zip` files.
- Back up and restore mod archives.

## Limitations

Prismite does not download Minecraft or mod loaders, launch the game, validate downloaded mod safety, or guarantee compatibility beyond Modrinth's version/loader metadata. Review project pages and use the official Minecraft launcher to play. The earlier `prismite-backend/` prototype remains unused; do not deploy it or use its unfinished OAuth code.

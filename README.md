# Prismite

Prismite is a local-only JavaFX Minecraft mod manager. It does not create accounts, sign in to Microsoft/Google/Discord, or launch Minecraft. Use the official Minecraft launcher to authenticate and play.

## Features

- Create isolated local instances or import an existing game directory.
- Keep instance metadata locally under `~/.prismite/instances.json`.
- Add/remove local `.jar` and `.zip` mod files.
- Search the public Modrinth API without an account and download a file compatible with the selected Minecraft version and loader, when a compatible release is available.
- Back up and restore mod archives as ZIP files.
- Best-effort loader hints from installed mod filenames/metadata; this is not authoritative detection of the Minecraft installation.

New isolated instance folders live under `~/.prismite/instances/`. Imported folders are not copied; Prismite manages the folder you explicitly select.

## Requirements and run

Install Java 17+ and Maven, then run:

```bash
cd prismite-client
mvn javafx:run
```

Modrinth browsing/install requires internet access. This app never asks for game account credentials and does not bypass Minecraft authentication or licensing.

## Limitations

- This app manages files; it does not install Minecraft or Fabric/Forge/Quilt, launch the game, validate mod archives, or guarantee that third-party mods are safe.
- Confirm each mod's project page, dependencies, game version, and loader before installing.
- Instance version/loader values are selected by the user. Loader inference from a mods folder is only a heuristic.
- The legacy `prismite-backend/` folder from an earlier prototype remains in the repository but is not used by this local client. Do not run or deploy that old OAuth prototype with real accounts; it contains unfinished/mock authentication code.

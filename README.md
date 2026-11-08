# Prismite

Prismite is a local-only Minecraft mod manager.

What it does:
- finds a Minecraft installation on your computer
- lets you choose a .minecraft folder manually
- lists the mods already installed in the mods folder
- adds a mod from your computer into the Minecraft mods folder
- removes mods from the folder
- opens the mods folder directly

What it does not do:
- no Prismite account
- no Google sign-in
- no Discord sign-in
- no Microsoft sign-in
- no fake OAuth or Discord guild join flow
- no Minecraft entitlement verification

This project is intentionally kept local-only and user-controlled.

## Run it

```bash
cd prismite-client
mvn javafx:run
```

## Notes

This app assumes you already own Minecraft Java Edition and that you run the official Minecraft launcher or install Minecraft separately.
It simply manages the mods folder in a Minecraft installation you choose.

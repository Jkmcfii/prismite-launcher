# Prismite Launcher

A practical starter for a Java launcher inspired by Modrinth-style launchers. It includes:

- Prismite account sign up/login
- Google, Discord, and Microsoft OAuth placeholders
- Discord guild join flow scaffold
- JavaFX launcher dashboard
- Instance management
- Mod browsing endpoint
- Local H2-backed backend

## Quick start

Backend:

```bash
cd prismite-backend
mvn spring-boot:run
```

Client:

```bash
cd prismite-client
mvn javafx:run
```

Then open the JavaFX app and sign in.

## Required environment variables

Copy `.env.example` to `.env` and fill in values when you have real app credentials.

```bash
cp .env.example .env
```

## Notes

This project intentionally keeps OAuth and Minecraft-launch logic in a production-ready scaffold form:

- real secrets are not committed
- providers can be configured via environment variables
- Discord guild join is implemented as a backend helper used after OAuth consent
- Minecraft launch logic is scaffolded in the launcher architecture, with real game launch integration left as a next step

## Included features

- Prismite user account auth
- OAuth provider placeholders for Google, Discord, and Microsoft
- Discord guild join helper (server-join after auth)
- launcher dashboard UI
- instance creation and listing
- mock Modrinth-style mod browser

## Production next steps

- Add Azure app registration for Microsoft
- Add Google Cloud OAuth app
- Add Discord app + bot + guild
- Add real Minecraft version manifest handling
- Add Java detection and launch command generation
- Add secure PostgreSQL persistence
- Add token verification and refresh flow

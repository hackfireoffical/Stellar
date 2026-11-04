# Project Stellar 🌌

A performance-first cinematic sky mod for **Minecraft 1.20.1 + Fabric**. The project aims for beautiful nights without becoming a heavyweight shaderpack.

## Current status

This repository contains the first development foundation: a Fabric client entrypoint, an `AFTER_SKY` render hook, a deterministic cached star field, a small JSON configuration, and a GitHub Actions build workflow. This is an early development build, not a finished shader replacement.

### Current features
- Cached procedural star positions (no per-frame random generation).
- Configurable star count and brightness.
- No custom framebuffer, bloom, temporal effects, or post-processing.
- Client-side only; no server installation required.

### Configuration
After launching once, edit `.minecraft/config/stellar.json`:

```json
{
  "enabled": true,
  "starCount": 450,
  "starBrightness": 0.82,
  "shootingStars": true,
  "cometChancePerSecond": 0.012
}
```

The comet options are reserved for the next implementation stage; the current build only renders stars. Restart Minecraft after editing.

## Build

Requires Java 17 and Gradle. Run:

```bash
gradle build
```

The mod JAR is written to `build/libs/`.

## Install

Install Fabric Loader for Minecraft 1.20.1, place the built `stellar-*.jar` in the `mods` folder, and launch with Fabric API installed.

## Compatibility and performance

The renderer deliberately uses a simple position-color pass and avoids owning the game's framebuffer. However, **MobileGL, OpenGL ES, Sodium, and frame-generation compatibility are not yet verified**. No FPS or compatibility claims should be made until tested on real devices and supported renderer combinations. The first milestone is to establish a working baseline, then benchmark each visual feature independently.

## Roadmap
- [x] Fabric 1.20.1 project foundation
- [x] Cached procedural star field
- [ ] Verify rendering order and sky orientation in-game
- [ ] Procedural Milky Way and nebula layers
- [ ] Shooting stars and comets
- [ ] Day/night and weather blending
- [ ] Configuration screen and quality presets
- [ ] Mali / MobileGL / Sodium compatibility testing
- [ ] Frame-generation compatibility testing

## License
MIT

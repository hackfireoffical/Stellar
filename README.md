# Project Stellar 🌌

A performance-first cinematic sky mod for **Minecraft 1.20.1 + Fabric**.

## Current status

Stellar injects its sky geometry at the end of Minecraft's overworld sky-render method, before terrain rendering.

### Implemented
- Client-only Fabric mod.
- Cached procedural star field, generated once and reused.
- Dense stars around a galactic band, with cool blue/lavender color variation.
- Three-layer segmented blue-violet Milky Way haze, drawn in the same geometry batch.
- Automatic fade-in at dusk and fade-out at dawn.
- Two-slot animated shooting-star pool with short cool-white tails; events are randomized and night-only.
- Configurable star count, star brightness, galaxy visibility, and galaxy brightness.
- No custom framebuffer, bloom, texture sampling, or post-processing.

### Configuration

After launching once, edit `.minecraft/config/stellar.json`:

```json
{
  "enabled": true,
  "starCount": 450,
  "starBrightness": 0.82,
  "galaxyEnabled": true,
  "galaxyBrightness": 0.75,
  "shootingStars": true,
  "cometChancePerSecond": 0.012
}
```

`shootingStars` toggles the animated streaks, and `cometChancePerSecond` controls their spawn chance. Restart Minecraft after editing.

## Build and install

Requires Java 17. GitHub Actions builds the mod and uploads the JAR as an artifact. Install Fabric Loader for Minecraft 1.20.1 and Fabric API, then place the JAR in the `mods` folder.

## Compatibility

The project compiles in CI, but visual behavior still needs in-game validation. **MobileGL, OpenGL ES, Sodium, Mali devices, and frame-generation compatibility are not yet verified.** The renderer avoids custom framebuffers and post-processing to keep GPU cost modest.

## Roadmap
- [x] Fabric 1.20.1 project foundation
- [x] Cached procedural star field
- [x] Dedicated sky-render hook
- [x] Low-cost Milky Way haze
- [x] Day/night fade
- [ ] Verify visual orientation, brightness, and render-state restoration in-game
- [ ] Nebula and distant galaxy detail
- [x] Lightweight shooting stars
- [ ] Weather blending
- [ ] Configuration screen and quality presets
- [ ] Mali / MobileGL / Sodium compatibility testing
- [ ] Frame-generation compatibility testing

## License
MIT

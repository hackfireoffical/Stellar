# Project Stellar 🌌

A performance-first cinematic sky mod for **Minecraft 1.20.1 + Fabric**.

## Current status

The project now uses a dedicated Mixin injection at the end of Minecraft's overworld sky-render method instead of the late world-render event. This puts Stellar's geometry in the sky pass, before terrain rendering.

### Implemented
- Client-only Fabric mod.
- Cached procedural star field, generated once and reused.
- Dense star distribution around a galactic band with subtle cool/lavender color variation.
- Configurable star count and brightness.
- One position-color draw batch; no custom framebuffer, bloom, texture sampling, or post-processing.

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

The comet options remain reserved for a later stage. Current rendering is stars only. Restart Minecraft after editing.

## Build and install

Requires Java 17. The GitHub Actions workflow builds the mod and uploads the JAR as an artifact. Install Fabric Loader for Minecraft 1.20.1 and Fabric API, then place the JAR in the `mods` folder.

## Compatibility

The code is build-tested, but the rendering still needs in-game validation. **MobileGL, OpenGL ES, Sodium, Mali devices, and frame-generation compatibility are not yet verified.** The renderer avoids custom framebuffers and post-processing to keep the GPU workload modest.

## Roadmap
- [x] Fabric 1.20.1 project foundation
- [x] Cached procedural star field
- [x] Dedicated sky-render hook
- [ ] Verify visual orientation, brightness, and render-state restoration in-game
- [ ] Milky Way haze and nebula layers
- [ ] Shooting stars and comets
- [ ] Day/night and weather blending
- [ ] Configuration screen and quality presets
- [ ] Mali / MobileGL / Sodium compatibility testing
- [ ] Frame-generation compatibility testing

## License
MIT

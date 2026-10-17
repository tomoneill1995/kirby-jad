# Kirby Jad

A RuneLite plugin that draws TzTok-Jad in the Fight Caves as Kirby and his Yt-HurKot healers
as Bandana Waddle Dees, with their own animations. Client-side only: nobody else sees it.
Only the Fight Caves Jad and healers are changed; Jads anywhere else are left alone.

## Installing the jar

Grab `kirby-jad-*.jar` from the [Releases](../../releases) page. It isn't on the Plugin Hub, so it
has to be sideloaded: put the jar in `%USERPROFILE%\.runelite\sideloaded-plugins` and start RuneLite
in developer mode (client argument `--developer-mode`, JVM argument `-ea`). Kirby Jad then shows up
in the plugin list. It's built against RuneLite 1.13.2, so a big client update can break it until it's
rebuilt.

## Building

- `run-test.bat` starts the locally built client (`..\runelite`) with the plugin loaded.
- `render-preview.bat` renders all animations to `build\preview\*.png` without the client.

## Dreamland caves

With **Dreamland caves** on, the Fight Caves become a Kirby-style meadow when the map loads. The
rocks, wall decorations and glowing cracks are stripped out, the floor becomes soft patchy grass,
lava becomes water with sandy edges and the sky turns blue. A puffy hedge runs round every tile you
can walk on (found by flood filling the collision map from you), with hills, Kirby mesas, apple
trees and a few Whispy Woods behind it, sized so nothing spills onto the path or into water. There
are flowers, butterflies, spinning warp stars, clouds and a rainbow over the arena. Anything that
had to be blanked rather than removed is put back when you leave, since TzHaar city shares models.

**Colour brightness** dims everything the plugin colours, for normal screens and recordings (HDR
monitors tone it down on their own).

## Reading Jad's attacks

| Jad does | Kirby does | Pray |
|---|---|---|
| Magic (rears up) | Puffs up taller, mouth opens wide within 0.2s, breathes fire | Mage |
| Ranged (slam) | Squashes flat with mouth shut, jumps, turns to grey stone, slams | Range |
| Melee | Twists and slaps with one arm | Melee |

The game's own projectiles, hitsplats, health bars and prayer overheads are unchanged.

Healers: heal = hop and wave the spear over their head; attack = spear jab; death = topple over.

## How it works

- The models are built in code (`mesh/`, `character/`): ellipsoid body parts plus decals that
  wrap the body surface for the eyes, cheeks and mouth, on a small bone rig with morph targets
  (blink, mouth open, smile). Lit per vertex every frame.
- RuneLite can't create models from scratch, so `ModelWriter` merges copies of Jad's cache model
  into a big enough host model and overwrites its vertices, faces and colours each frame; unused
  faces are hidden (colour3 = -2).
- The real NPC's 3D model is hidden with a renderable draw listener (UI elements still draw), and
  a `RuneLiteObjectController` with the replacement follows it. Hidden NPCs can't be clicked, so
  the NPC's own options (Attack, Examine...) are added to the menu while the mouse is over the
  replacement.
- NPCs are matched by id (Fight Caves Jad 3127 and its clan cup copy 6506, healer 3128), not
  by name, so other Jads and healers are never touched. Animation ids are mapped in
  `NpcAnimations`; durations come from the cache (`CacheProbe`).

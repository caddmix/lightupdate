lightupdate

a client side fabric mod for minecraft 1.21.1 that removes the hard light steps on mobs, players and block entities.

vanilla reads one light value per entity from the block its eyes sit in, so a mob walking toward a torch gets brighter in whole level jumps, one jump at every block border, while the ground under it fades smoothly. on top of that the 1.21.1 entity shaders read the lightmap with texelFetch, which throws away everything between whole light levels, so smoothing on the cpu alone would still show steps.

what the mod does

entities blend the light of the 8 blocks around their eye position, weighted by distance to each block center. solid blocks with zero light drop out of the blend, so mobs next to walls or with their eyes low over the floor do not go darker. light now follows the entity as it moves instead of jumping at block borders.

entities and block entities fade toward new light over about a quarter second, so a torch placed or broken next to a mob or a chest fades instead of snapping. the first frame something shows up and teleports snap straight to the right value.

the vanilla entity, armor, item and text shaders get a filtered lightmap lookup centered on lightmap texels. whole light levels hit texel centers, so vanilla light keeps its exact color, and the values between levels now show up. the swap only fires on the exact vanilla line, so a resource pack with its own entity shaders keeps its version. iris shader packs use their own programs and read the light coordinates as floats, so they get the smooth values without any swap.

your first person hand and held item use the same entity light, so they fade too. leashes, paintings, particles, entity shadows and terrain keep vanilla light.

install

minecraft 1.21.1, fabric loader 0.15.11 or newer, fabric api. client only, servers do not need it. drop the jar into the mods folder.

toggle key

controls, lightupdate, toggle smooth light. unbound by default, bind it to flip between vanilla and smooth light live.

config

config/lightupdate.json, read at startup.

enabled true turns the whole mod on or off.
entities true smooths mobs, players, item frames and every other entity.
block_entities true fades chests, signs, banners, beds and the rest.
spatial_blend true blends the 8 blocks around an entity, false keeps only the time fade.
fade_ms 250 is the time a light change needs to settle, 0 turns the fade off.
patch_shaders true swaps the lightmap lookup in vanilla entity shaders, needs f3 t to change.
debug_stats false adds an f3 line with updates per second, resample share and nanoseconds per entity.

performance

the 8 corner values for an entity pack into one long cached on the entity. the world only gets read again when the entity moves into a new cell or the light engine reports a change next to it, plus a backstop every 2 seconds. an entity that stands still with settled light skips all math. there are no allocations, no hash maps and no locks on the per frame path, and vanilla keeps its own two light reads so dynamic light mods keep working.

measured in a dev client on a 4 core vm with software opengl, 776 villagers on screen took about 70 to 100 ns per entity per frame after subtracting the timer cost, around 0.5% of a 60 fps frame. fps showed no drop with the mod on.

compatibility

tested in a production style launch with sodium 0.8.13, iris 1.8.14 beta 1 (shaders off, and on with bsl 10.1.8), immediatelyfast 1.6.15, entityculling 1.11.3, firelight 1.0.2, entity texture features 7.2.6, entity model features 3.3.12, scalablelux 0.3.0 alpha 0.7, badoptimizations 2.4.1 and better block entities 1.3.4. all hooks are mixinextras injections at a lower priority than the default, so mods that change entity light after vanilla, like firelight flicker or etf overrides, apply on top of the smooth value.

better block entities bakes chests, signs, beds, banners, bells, shulker boxes and decorated pots into terrain while they do not animate. those use terrain light and snap like terrain, the fade only covers them while they animate or when better block entities leaves that type alone.

the light engine change callback works with vanilla and scalablelux. the mod also covers the neighbor sections itself, because scalablelux only reports the changed ones.

building

java 21, then ./gradlew build. the jar lands in build/libs.

./gradlew test runs the unit tests, one of them runs the real 1.21.1 entity shaders from the game jar through the shader swap.

./gradlew runHarness -Pscene=candle opens a dev client, builds a torch tunnel, walks the player to the torch and back with the mod off and on, and writes a per frame csv to run-harness/harness. other scenes are sweep (quarter block steps across block borders, works at any frame rate), lamp (a torch next to a double chest turning on and off) and crowd (776 villagers for cost numbers). the harness is a separate source set and never ships in the mod jar.

license

mit

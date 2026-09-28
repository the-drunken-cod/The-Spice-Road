<div align="center" style="text-align: center;">

# The Spice Road
Adds rare spices to Minecraft that can enhance existing foods with a new buff mechanic and encourages local production and trade.

![Banner](./.github/assets/banner.png)

—  

<sup>Available on <a href="https://github.com/the-drunken-cod/The-Spice-Road/releases">GitHub</a>, <a href="https://modrinth.com/mod/the-spice-road">Modrinth</a> & <a href="https://www.curseforge.com/minecraft/mc-mods/the-spice-road">CurseForge</a></sup>

---

### [Wiki](https://github.com/the-drunken-cod/The-Spice-Road/wiki) &bull; [Features](#features) &bull; [Installation](#installation) &bull; [Integrations](#integrations) &bull; [Attribution](#attribution) &bull; [License](#license) &bull; [Disclaimers](#disclaimers)

---

</div>

<br>

## Features:
- **30 new spice plants now generate in dedicated regions throughout the overworld.** They are dependent on climate, but not bound to biomes and their size is configurable.
- **Common spices may be taken back home and grown**, but a good portion of them will **only mature if produced locally.**  
  This incentivizes exploration, as well as establishing trade routes on servers. Maps can be traded for or found to help find certain spices.
- New mechanic that allows **spices to be mixed and matched and applied to existing foods** of any mod to **imbue them with beneficial custom effects.** Players can also create spice mixes and specialize in cultivating the rarest spice plants.
- **Incredibly modpack-, datapack- and server admin friendly.**
  - Allows for tons of configuration for any play style and player base.
  - Spice profiles and items can easily be added by datapack makers, and any mod's food item is supported out of the box. Note: some mods' crafting stations might erase spice profiles. In that case, please [open an issue](https://github.com/the-drunken-cod/The-Spice-Road/issues) so we can add compatibility.
  - Compatibility datapacks can easily add spice profiles to any other mods' items. Refer to the [spice profile documentation](./docs/data/spice_profile.md) for more info.
  - Commands for finding (`/locate spice`) and utilities for shuffling spice regions for balancing or setting up scenarios.

<br>

## Installation:
You can visit the [releases page](https://github.com/the-drunken-cod/The-Spice-Road/releases), the [TODO: Modrinth page](), or the [TODO: CurseForge page]() to download the latest version of the mod.  
Then simply place the downloaded JAR file into your Minecraft `mods` folder and launch the game with either NeoForge or Fabric.  
  
> [!IMPORTANT]  
> The **Fabric version** requires [Cloth Config API](https://modrinth.com/mod/cloth-config) to be installed as well.

> [!NOTE]  
> You will need either the [NeoForge](https://neoforge.dev/) or [Fabric](https://fabricmc.net/) mod loader installed to run the mod.  
> An easy way of doing this is by creating a modded game instance in a launcher like [ATLauncher](https://atlauncher.com/) or [MultiMC.](https://multimc.org/)

<br>

## Integrations:
- [Farmer's Delight](https://modrinth.com/mod/farmers-delight)
  - Recipe compatibility for the cooking pot and cutting board (ingredient spice profiles get correctly summed for the outputs).
  - Tag-level compatibility (knives as cutting tool, etc.).
- [Cooking For Blockheads](https://modrinth.com/mod/cooking-for-blockheads)
  - Recipe compatibility for the toaster and oven (ingredient spice profiles get correctly summed for the outputs).
- [Serene Seasons](https://modrinth.com/mod/serene-seasons)
  - Spice crops have realistic season data attached to them, with plants growing throughout the whole year.
- [Create](https://modrinth.com/mod/create)
  - Spices can be harvested via deployers.
- [Sable](https://modrinth.com/mod/sable) / [Create Aeronautics](https://modrinth.com/mod/create-aeronautics)
  - Sublevels sample the spice region from the correct overworld coordinates.
  - TODO: adjusted block weights.

<br>

## Attribution:
- Created from the template [jaredlll08/MultiLoader-Template.](https://github.com/jaredlll08/MultiLoader-Template)  
  Changes:
  - Added build and release GitHub Actions workflows.
  - Added VS Code settings and debug launch configurations.
  - Added base config services for Neo and Fabric.
  - Added a base registry helper for Neo and Fabric.
  - Added base datagen services for Neo and Fabric.

<br>

## License:
This project is licensed under the LGPL-3.0-or-later license.  
See the [`LICENSE.txt` file](https://github.com/the-drunken-cod/The-Spice-Road/blob/develop/LICENSE.txt) for details.

<br>

## Disclaimers:
- We use Generative AI when coding to make tedious work easier and bridge knowledge gaps for this hobby project. [You can read our full policy here.](https://github.com/the-drunken-cod#genai-usage) That being said, **every generated line is reviewed**, and **all assets remain fully human-made.**
- NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.  
- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

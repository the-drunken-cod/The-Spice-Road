## The Spice Road - CLI Tools
These tools are to be used in the command line and offer various shortcuts:

<br>

- **`pnpm extract_prop <fileIdentifier> <propName>`**: Echoes the value of the given property from the `gradle.properties` or the version-dependent properties files like `1.21.1`.
- **`pnpm create_placeholder_texture <args>`**: Creates a placeholder PNG texture, including custom text, colors and even a binary counter.
    - Required:
        - `-O` / `--output`: Path to the output file. Parent directories are created and existing files are overwritten. If no other args are specified, the texture is 16x16 and fully black.
    - Optional:
        - `-T` / `--text`: Text content rendered in the center in an aliased monospaced 3x5 pixel font.  
          Supports `A-Z` (case-insensitive), `0-9` and ` .,:!?+-_=*\/()#`. The font is scaled up by whole numbers to fit the texture. Unsupported characters are rendered as a solid box. Defaults to undefined (off).
        - `-W` / `--width`: Width of the texture, defaults to 16.
        - `-H` / `--height`: Height of the texture, defaults to 16.
        - `-C` / `--color`: Text and counter color as an rgb or rgba hex code, defaults to `#ffffffff`.
        - `-B` / `--background`: Background color as an rgb or rgba hex code, defaults to `#000000ff`.
        - `-N` / `--number`: Non-negative integer drawn on the top row as a binary counter (one pixel per bit, least significant bit on the right). Must fit into `width` bits. The text is centered below it. Defaults to undefined (off).
        - `--help`: Prints the usage.
- **`pnpm pad_csv ./in.csv ./out.csv`**: Makes a CSV look better by aligning its cells horizontally.
- **`pnpm spice_balancer`**: Opens a local web tool for rebalancing the spice profiles in `data/spice_road/spice_profile/`.
    - Spices are listed by Tier, with their harvest difficulty, climate and growing seasons (`SP`, `SU`, `AU`, `WI`) and one slider per Flavor Axis.
    - The Potency (sum of absolute axis values) is shown per spice and per Tier. "Scale" multiplies a whole profile so it hits its Tier's target Potency while keeping its shape.
    - Tick the checkbox on up to 4 spices to compare and edit them side by side.
    - Edits and notes are kept in persistent browser storage until they're reset. "Export ZIP" downloads only the changed JSONs at their datapack paths, ready to be copied over `common/src/main/resources/`.
    - "Load folder..." uses another set of profile JSONs (e.g. a datapack) as the baseline to compare against.
    - Tiers, climates and seasons come from `dev/spice_meta.json`, which is written by datagen. Run datagen after changing a Spice's metadata or Java enum values.
    - Slider range, step and the target Potency per Tier are constants in `tools/spice_balancer/src/config.ts`.

<br>

## License
The files in this `tools/` folder are licensed under the MIT License.  
Feel free to use these scripts for your own projects, as long as The Spice Road is credited.  
  
Refer to the [`LICENSE.txt` file](./LICENSE.txt) for more details.

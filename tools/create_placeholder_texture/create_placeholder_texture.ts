import { mkdirSync, writeFileSync } from "node:fs";
import { dirname } from "node:path";
import { parseArgs } from "node:util";
import { PNG } from "pngjs";

/**
 * Creates a placeholder texture at the given path. Supports arguments to render simple characters.
 * Required args:
 * - `-O` / `--output`: Path to the output file. Parent directories are created and existing files are overwritten.
 *
 * Optional args:
 * - `-T` / `--text`: Text content rendered in the center in an aliased monospaced 3x5 pixel font. Supports `A-Z` (case-insensitive), `0-9` and ` .,:!?+-_=*\/()#`. The font is scaled up by whole numbers to fit the texture. Unsupported characters are rendered as a solid box. Defaults to undefined (off).
 * - `-W` / `--width`: Width of the texture, defaults to 16.
 * - `-H` / `--height`: Height of the texture, defaults to 16.
 * - `-C` / `--color`: Text and counter color as an rgb or rgba hex code, defaults to #ffffffff.
 * - `-B` / `--background`: Background color as an rgb or rgba hex code, defaults to #000000ff.
 * - `-N` / `--number`: Non-negative integer drawn on the top row as a binary counter (one pixel per bit, least significant bit on the right). Must fit into `width` bits. The text is centered below it. Defaults to undefined (off).
 * - `--help`: Prints the usage.
 *
 * Values starting with a dash need the `--option=value` syntax.
 * @example
 * ```
 * pnpm run create_placeholder_texture -- -O ./out.png -T "A1" -N 5 -B "#202020"
 * ```
 */

// #region font

/** Glyph width in font pixels */
const glyphWidth = 3;
/** Glyph height in font pixels */
const glyphHeight = 5;
/** Empty font pixels between two glyphs */
const glyphGap = 1;

/** Glyphs of the 3x5 font. Each string is 5 rows of 3 bits (`1` = set), top to bottom. */
const font: Record<string, string> = {
  "0": "111101101101111", "1": "010110010010111", "2": "111001111100111", "3": "111001111001111",
  "4": "101101111001001", "5": "111100111001111", "6": "111100111101111", "7": "111001001010010",
  "8": "111101111101111", "9": "111101111001111",
  "A": "010101111101101", "B": "110101110101110", "C": "011100100100011", "D": "110101101101110",
  "E": "111100110100111", "F": "111100110100100", "G": "011100101101011", "H": "101101111101101",
  "I": "111010010010111", "J": "001001001101010", "K": "101101110101101", "L": "100100100100111",
  "M": "101111111101101", "N": "110101101101101", "O": "010101101101010", "P": "110101110100100",
  "Q": "010101101110011", "R": "110101110101101", "S": "011100010001110", "T": "111010010010010",
  "U": "101101101101111", "V": "101101101101010", "W": "101101111111101", "X": "101101010101101",
  "Y": "101101010010010", "Z": "111001010100111",
  " ": "000000000000000", ".": "000000000000010", ",": "000000000010100", ":": "000010000010000",
  "!": "010010010000010", "?": "110001010000010", "+": "000010111010000", "-": "000000111000000",
  "_": "000000000000111", "=": "000111000111000", "*": "000101010101000", "/": "001001010100100",
  "(": "001010010010001", ")": "100010010010100", "#": "101111101111101",
};

/** Glyph used for characters missing in {@linkcode font} */
const fallbackGlyph = "111111111111111";

// #region helpers

type Rgba = [r: number, g: number, b: number, a: number];

/**
 * Parses a `rgb`, `rgba`, `rrggbb` or `rrggbbaa` hex code, with or without leading `#`.
 * @param value The hex code.
 * @param name Option name used in the error message.
 * @return The parsed channels, alpha defaults to 255.
 */
function parseColor(value: string, name: string): Rgba {
  let hex = value.replace(/^#/, "");
  if(/^[0-9a-f]{3,4}$/i.test(hex))
    hex = [...hex].map((c) => c + c).join("");
  if(!/^([0-9a-f]{6}|[0-9a-f]{8})$/i.test(hex))
    fail(`Invalid ${name} '${value}', expected a hex code like #rgb, #rgba, #rrggbb or #rrggbbaa`);
  const ch = (i: number) => parseInt(hex.slice(i * 2, i * 2 + 2), 16);
  return [ch(0), ch(1), ch(2), hex.length === 8 ? ch(3) : 255];
}

/**
 * Parses a positive integer option.
 * @param value The raw value.
 * @param name Option name used in the error message.
 * @return The parsed integer.
 */
function parseSize(value: string, name: string): number {
  if(!/^\d+$/.test(value) || Number(value) < 1 || Number(value) > 4096)
    fail(`Invalid ${name} '${value}', expected an integer from 1 to 4096`);
  return Number(value);
}

/**
 * Prints an error and exits with code 1.
 * @param message The error message.
 */
function fail(message: string): never {
  console.error(`Error: ${message}`);
  process.exit(1);
}

// #region main

const { values } = (() => {
  try {
    return parseArgs({
      options: {
        output:     { type: "string", short: "O" },
        text:       { type: "string", short: "T" },
        width:      { type: "string", short: "W", default: "16" },
        height:     { type: "string", short: "H", default: "16" },
        color:      { type: "string", short: "C", default: "#ffffffff" },
        background: { type: "string", short: "B", default: "#000000ff" },
        number:     { type: "string", short: "N" },
        help:       { type: "boolean" },
      },
      strict: true,
    });
  }
  catch(err) {
    return fail(err instanceof Error ? err.message : String(err));
  }
})();

if(values.help || !values.output) {
  console.error("Usage: create_placeholder_texture -O <out.png> [-T text] [-W 16] [-H 16] [-C #ffffffff] [-B #000000ff] [-N number]");
  process.exit(values.help ? 0 : 1);
}

const width = parseSize(values.width, "width");
const height = parseSize(values.height, "height");
const color = parseColor(values.color, "color");
const background = parseColor(values.background, "background");

const png = new PNG({ width, height });

/**
 * Sets a single pixel, ignoring out-of-bounds coordinates.
 * @param x Pixel column.
 * @param y Pixel row.
 * @param rgba The color to write (no blending).
 */
function setPixel(x: number, y: number, rgba: Rgba): void {
  if(x < 0 || y < 0 || x >= width || y >= height)
    return;
  const idx = (y * width + x) * 4;
  png.data[idx] = rgba[0];
  png.data[idx + 1] = rgba[1];
  png.data[idx + 2] = rgba[2];
  png.data[idx + 3] = rgba[3];
}

for(let y = 0; y < height; y++)
  for(let x = 0; x < width; x++)
    setPixel(x, y, background);

// #region number

let areaTop = 0;

if(values.number !== undefined) {
  if(!/^\d+$/.test(values.number))
    fail(`Invalid number '${values.number}', expected a non-negative integer`);
  const num = BigInt(values.number);
  if(num >= 1n << BigInt(width))
    fail(`Number ${values.number} doesn't fit into ${width} bits (max ${(1n << BigInt(width)) - 1n})`);
  for(let bit = 0; bit < width; bit++)
    if((num >> BigInt(bit)) & 1n)
      setPixel(width - 1 - bit, 0, color);
  areaTop = 1;
}

// #region text

if(values.text) {
  const chars = [...values.text.toUpperCase()];
  const textW = chars.length * (glyphWidth + glyphGap) - glyphGap;
  const areaH = height - areaTop;
  const scale = Math.max(1, Math.min(Math.floor(width / textW), Math.floor(areaH / glyphHeight)));
  if(textW * scale > width || glyphHeight * scale > areaH)
    console.warn(`Warning: text '${values.text}' doesn't fit into ${width}x${areaH} pixels and is clipped`);
  const originX = Math.floor((width - textW * scale) / 2);
  const originY = areaTop + Math.floor((areaH - glyphHeight * scale) / 2);
  chars.forEach((char, ci) => {
    const glyph = font[char] ?? fallbackGlyph;
    for(let gy = 0; gy < glyphHeight; gy++)
      for(let gx = 0; gx < glyphWidth; gx++)
        if(glyph[gy * glyphWidth + gx] === "1")
          for(let sy = 0; sy < scale; sy++)
            for(let sx = 0; sx < scale; sx++)
              setPixel(originX + (ci * (glyphWidth + glyphGap) + gx) * scale + sx, originY + gy * scale + sy, color);
  });
}

// #region write

mkdirSync(dirname(values.output), { recursive: true });
writeFileSync(values.output, PNG.sync.write(png));
console.log(`wrote ${values.output} (${width}x${height})`);

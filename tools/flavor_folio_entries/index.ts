import { existsSync, mkdirSync, readdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join, relative, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { parseArgs } from "node:util";

/**
 * Stamps a skeleton Flavor Folio entry for every raw Spice in `dev/spice_meta.json` that doesn't have one yet,
 * sorted into the category of its Tier. Existing entries are never touched, so hand-written articles are safe
 * and the tool can be re-run whenever a Spice is added.
 *
 * The Spice Profile pages of a skeleton point to the Spice's Processed Spices (read from `ProcessedSpice.java`), as
 * only those carry a profile; Spices without any use the raw item.
 *
 * Options:
 * - `-L` / `--lang`: Locale folder of the book to stamp into, defaults to `en_us`.
 * - `--check`: Writes nothing. Lists Spices without an entry, entries of unknown Spices and entries sitting in the
 *   category of another Tier than their Spice's. Exits with code 1 if there is anything to report.
 * - `--help`: Prints the usage.
 * @example
 * ```
 * pnpm flavor_folio_entries
 * pnpm flavor_folio_entries --check
 * ```
 */

// #region types

/** An item of `spice_meta.json`. */
interface MetaItem {
  /** Item ID of the Spice Item */
  item: string;
  /** ID of the Spice the item belongs to */
  spice: string;
  /** Set for processed items, which get no entry of their own */
  processing?: string;
  tier: string;
}

/** A raw Spice and the Tier it is listed under. */
interface RawSpice {
  id: string;
  tier: string;
}

// #region paths

const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..", "..");
const metaPath = join(repoRoot, "dev", "spice_meta.json");
const langPath = join(repoRoot, "common", "src", "main", "resources", "assets", "spice_road", "lang", "en_us.json");
const processedPath = join(repoRoot, "common", "src", "main", "java", "com", "drunkencod", "spice_road", "spice", "ProcessedSpice.java");
const bookPath = join(repoRoot, "common", "src", "main", "resources", "assets", "spice_road", "patchouli_books", "flavor_folio");

// #region helpers

/** @returns Every raw Spice in `spice_meta.json`, in file order. */
function readRawSpices(): RawSpice[] {
  const meta = JSON.parse(readFileSync(metaPath, "utf8")) as { items: MetaItem[] };
  return meta.items.filter(item => item.processing === undefined).map(item => ({ id: item.spice, tier: item.tier }));
}

/** @returns The item IDs of the Processed Spices in `ProcessedSpice.java`, grouped by the ID of the Spice they are made from. */
function readProcessedItems(): Map<string, string[]> {
  const bySpice = new Map<string, string[]>();
  const source = readFileSync(processedPath, "utf8");
  for (const match of source.matchAll(/^\s*[A-Z_]+\("([a-z_]+)",\s*Spice\.([A-Z_]+),/gm)) {
    const spice = match[2]!.toLowerCase();
    bySpice.set(spice, [...(bySpice.get(spice) ?? []), match[1]!]);
  }
  return bySpice;
}

/**
 * @param spice The Spice.
 * @returns Its English name from the lang file, or a title-cased version of its ID if the lang file has none.
 */
function displayName(spice: RawSpice, lang: Record<string, string>): string {
  return lang[`item.spice_road.${spice.id}`]
    ?? spice.id.split("_").map(word => word.charAt(0).toUpperCase() + word.slice(1)).join(" ");
}

/**
 * @param spice The Spice.
 * @param name Its display name.
 * @returns The JSON text of its skeleton entry.
 */
function skeleton(spice: RawSpice, name: string, processed: string[]): string {
  // Spice Profiles sit on the Processed Spices if the Spice has any, else on the raw item
  const profileItems = processed.length > 0 ? processed : [spice.id];
  return JSON.stringify({
    name,
    icon: `spice_road:${spice.id}`,
    category: `spice_road:spices_${spice.tier}`,
    pages: [
      { type: "patchouli:text", title: name, text: `TODO: write the article for ${name}.` },
      { type: "spice_road:spice_info", spice: spice.id },
      ...profileItems.map(item => ({ type: "spice_road:spice_profile", item: `spice_road:${item}` })),
    ],
  }, null, 4) + "\n";
}

/**
 * @param dir The folder to search.
 * @returns The IDs and Tier folder names of all entries found in `entries/spices/<tier>/`.
 */
function existingEntries(dir: string): { id: string; tier: string }[] {
  const found: { id: string; tier: string }[] = [];
  if (!existsSync(dir))
    return found;
  for (const tier of readdirSync(dir, { withFileTypes: true }).filter(entry => entry.isDirectory())) {
    for (const file of readdirSync(join(dir, tier.name)).filter(name => name.endsWith(".json")))
      found.push({ id: file.slice(0, -".json".length), tier: tier.name });
  }
  return found;
}

// #region main

function main() {
  const { values } = parseArgs({
    options: {
      lang: { type: "string", short: "L", default: "en_us" },
      check: { type: "boolean", default: false },
      help: { type: "boolean", default: false },
    },
  });
  if (values.help) {
    console.log("Usage: pnpm flavor_folio_entries [-L <locale>] [--check]");
    return;
  }

  const spices = readRawSpices();
  const processed = readProcessedItems();
  const lang = JSON.parse(readFileSync(langPath, "utf8")) as Record<string, string>;
  const entriesDir = join(bookPath, values.lang, "entries", "spices");
  const existing = existingEntries(entriesDir);
  const problems: string[] = [];

  for (const spice of spices) {
    const path = join(entriesDir, spice.tier, `${spice.id}.json`);
    if (existsSync(path))
      continue;
    const misplaced = existing.find(entry => entry.id === spice.id);
    if (misplaced) {
      problems.push(`${spice.id} is listed under "${misplaced.tier}" but its Spice is "${spice.tier}"`);
      continue;
    }
    if (values.check) {
      problems.push(`${spice.id} has no entry`);
      continue;
    }
    mkdirSync(dirname(path), { recursive: true });
    writeFileSync(path, skeleton(spice, displayName(spice, lang), processed.get(spice.id) ?? []), "utf8");
    console.log(`created ${relative(repoRoot, path)}`);
  }
  for (const entry of existing) {
    if (!spices.some(spice => spice.id === entry.id))
      problems.push(`${entry.tier}/${entry.id} is not a Spice in spice_meta.json`);
  }

  for (const problem of problems)
    console.warn(`warning: ${problem}`);
  if (values.check && problems.length > 0)
    process.exitCode = 1;
}

main();

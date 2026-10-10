import { MAX_COMPARE, STORAGE_PREFIX } from "./config";
import { parseProfile, type Profile, type ProfileFile } from "./data";
import { profilesEqual } from "./potency";

/** An externally loaded baseline as persisted in localStorage. */
interface StoredBaseline {
  name: string;
  files: Array<{ path: string; raw: string }>;
}

/** How long writes to localStorage are held back while values keep changing. */
const SAVE_DELAY_MS = 250;

/**
 * Holds the baseline profiles, the edits made on top of them, notes and the
 * compare selection, and keeps everything but the repository baseline in
 * localStorage.
 */
export class Store {
  readonly axes: readonly string[];

  private readonly repoFiles: ProfileFile[];
  private baseline = new Map<string, ProfileFile>();
  private baselineName: string | null = null;
  private edits = new Map<string, Profile>();
  private notes = new Map<string, string>();
  private selected: string[] = [];

  private readonly profileListeners = new Set<(path: string) => void>();
  private readonly structureListeners = new Set<() => void>();
  private readonly selectionListeners = new Set<() => void>();
  private saveTimer: number | undefined;

  /**
   * @param axes      Axis IDs in display order.
   * @param repoFiles The profiles from the repository, the default baseline.
   */
  constructor(axes: readonly string[], repoFiles: ProfileFile[]) {
    this.axes = axes;
    this.repoFiles = repoFiles;
    this.restore();
  }

  // #region listeners

  /**
   * @param listener Called with a profile's path whenever its values change.
   * @return A function removing the listener.
   */
  onProfile(listener: (path: string) => void): () => void {
    this.profileListeners.add(listener);
    return () => this.profileListeners.delete(listener);
  }

  /**
   * @param listener Called whenever the set of profiles changes.
   * @return A function removing the listener.
   */
  onStructure(listener: () => void): () => void {
    this.structureListeners.add(listener);
    return () => this.structureListeners.delete(listener);
  }

  /**
   * @param listener Called whenever the compare selection changes.
   * @return A function removing the listener.
   */
  onSelection(listener: () => void): () => void {
    this.selectionListeners.add(listener);
    return () => this.selectionListeners.delete(listener);
  }

  // #region baseline

  /** @return The baseline profile files, sorted by path. */
  files(): ProfileFile[] {
    return [...this.baseline.values()].sort((a, b) => a.path.localeCompare(b.path));
  }

  /**
   * @param path A profile's path.
   * @return Its baseline file, if loaded.
   */
  file(path: string): ProfileFile | undefined {
    return this.baseline.get(path);
  }

  /** @return The name of the external baseline, or {@code null} while the repository files are used. */
  getBaselineName(): string | null {
    return this.baselineName;
  }

  /**
   * Replaces the baseline. Edits are kept by path and now compare against the new baseline.
   * @param files The new baseline files.
   * @param name  The external set's name, or {@code null} for the repository files.
   */
  setBaseline(files: ProfileFile[], name: string | null): void {
    this.baseline = new Map(files.map(file => [file.path, file]));
    this.baselineName = name;
    this.selected = this.selected.filter(path => this.baseline.has(path));
    this.pruneEdits();
    this.saveNow();
    this.emitStructure();
  }

  /** Switches back to the repository files as the baseline. */
  useRepoBaseline(): void {
    this.setBaseline(this.repoFiles, null);
  }

  // #region profiles

  /**
   * @param path A profile's path.
   * @return Its baseline values, with missing axes as 0.
   */
  getDefault(path: string): Profile {
    return this.fill(this.baseline.get(path)?.profile ?? {});
  }

  /**
   * @param path A profile's path.
   * @return Its current values, with missing axes as 0.
   */
  get(path: string): Profile {
    const edit = this.edits.get(path);
    return edit ? this.fill(edit) : this.getDefault(path);
  }

  /**
   * @param path  A profile's path.
   * @param axis  The axis to change.
   * @param value The new value.
   */
  setAxis(path: string, axis: string, value: number): void {
    this.setProfile(path, { ...this.get(path), [axis]: value });
  }

  /**
   * @param path    A profile's path.
   * @param profile Its new values.
   */
  setProfile(path: string, profile: Profile): void {
    if (profilesEqual(profile, this.getDefault(path), this.axes))
      this.edits.delete(path);
    else
      this.edits.set(path, this.fill(profile));
    this.scheduleSave();
    this.emitProfile(path);
  }

  /** @param path A profile's path, reset to its baseline values. */
  reset(path: string): void {
    if (this.edits.delete(path)) {
      this.scheduleSave();
      this.emitProfile(path);
    }
  }

  /** Discards every edit. */
  resetAll(): void {
    const paths = [...this.edits.keys()];
    this.edits.clear();
    this.saveNow();
    paths.forEach(path => this.emitProfile(path));
  }

  /**
   * @param path A profile's path.
   * @return Whether it differs from its baseline.
   */
  isChanged(path: string): boolean {
    return this.edits.has(path) && this.baseline.has(path);
  }

  /** @return The paths of all changed profiles in the current baseline. */
  changedPaths(): string[] {
    return [...this.edits.keys()].filter(path => this.baseline.has(path)).sort();
  }

  // #region notes

  /**
   * @param path A profile's path.
   * @return Its note, or an empty string.
   */
  getNote(path: string): string {
    return this.notes.get(path) ?? "";
  }

  /**
   * @param path A profile's path.
   * @param note Its new note; blank notes are removed.
   */
  setNote(path: string, note: string): void {
    if (note.trim())
      this.notes.set(path, note);
    else
      this.notes.delete(path);
    this.scheduleSave();
  }

  // #region selection

  /** @return The paths selected for comparison, in selection order. */
  getSelected(): readonly string[] {
    return this.selected;
  }

  /**
   * @param path A profile's path.
   * @return Whether it can't be selected because the compare view is full.
   */
  isSelectionFull(path: string): boolean {
    return !this.selected.includes(path) && this.selected.length >= MAX_COMPARE;
  }

  /**
   * @param path     A profile's path.
   * @param selected Whether it should be compared.
   */
  setSelected(path: string, selected: boolean): void {
    const index = this.selected.indexOf(path);
    if (selected && index < 0 && this.selected.length < MAX_COMPARE)
      this.selected = [...this.selected, path];
    else if (!selected && index >= 0)
      this.selected = this.selected.filter(other => other !== path);
    else
      return;
    this.saveNow();
    this.selectionListeners.forEach(listener => listener());
  }

  // #region persistence

  /** Loads the persisted state, falling back to the repository baseline. */
  private restore(): void {
    this.edits = new Map(Object.entries(readJson<Record<string, Profile>>("edits") ?? {}));
    this.notes = new Map(Object.entries(readJson<Record<string, string>>("notes") ?? {}));
    this.selected = readJson<string[]>("selected") ?? [];

    const stored = readJson<StoredBaseline>("baseline");
    const files = stored?.files
      .map(file => parseProfile(file.path, file.raw))
      .filter((file): file is ProfileFile => file !== null);

    this.baseline = new Map((files?.length ? files : this.repoFiles).map(file => [file.path, file]));
    this.baselineName = files?.length ? stored!.name : null;
    this.selected = this.selected.filter(path => this.baseline.has(path)).slice(0, MAX_COMPARE);
    this.pruneEdits();
  }

  /** Drops edits that match their baseline, e.g. after the repository files caught up with them. */
  private pruneEdits(): void {
    for (const [path, edit] of this.edits) {
      if (this.baseline.has(path) && profilesEqual(edit, this.getDefault(path), this.axes))
        this.edits.delete(path);
    }
  }

  /** Saves after a short delay, so dragging a slider doesn't write on every input. */
  private scheduleSave(): void {
    window.clearTimeout(this.saveTimer);
    this.saveTimer = window.setTimeout(() => this.saveNow(), SAVE_DELAY_MS);
  }

  /** Saves the persisted state right away. */
  private saveNow(): void {
    window.clearTimeout(this.saveTimer);
    writeJson("edits", Object.fromEntries(this.edits));
    writeJson("notes", Object.fromEntries(this.notes));
    writeJson("selected", this.selected);
    writeJson("baseline", this.baselineName === null
      ? null
      : { name: this.baselineName, files: this.files().map(({ path, raw }) => ({ path, raw })) } satisfies StoredBaseline);
  }

  // #region helpers

  /**
   * @param profile A possibly sparse profile.
   * @return A copy holding every known axis, missing ones as 0, plus any unknown axes it had.
   */
  private fill(profile: Profile): Profile {
    const filled: Profile = { ...profile };
    for (const axis of this.axes)
      filled[axis] ??= 0;
    return filled;
  }

  /** @param path The profile whose values changed. */
  private emitProfile(path: string): void {
    this.profileListeners.forEach(listener => listener(path));
  }

  /** Notifies listeners of a change to the set of profiles. */
  private emitStructure(): void {
    this.structureListeners.forEach(listener => listener());
  }
}

// #region storage

/**
 * @param key The key, without prefix.
 * @return The parsed value, or {@code null} if it's missing, unreadable or storage is unavailable.
 */
function readJson<T>(key: string): T | null {
  try {
    const raw = localStorage.getItem(STORAGE_PREFIX + key);
    return raw ? JSON.parse(raw) as T : null;
  }
  catch {
    return null;
  }
}

/**
 * @param key   The key, without prefix.
 * @param value The value to store; {@code null} removes the key.
 */
function writeJson(key: string, value: unknown): void {
  try {
    if (value === null)
      localStorage.removeItem(STORAGE_PREFIX + key);
    else
      localStorage.setItem(STORAGE_PREFIX + key, JSON.stringify(value));
  }
  catch (err) {
    console.warn(`Couldn't save "${key}" to localStorage:`, err);
  }
}

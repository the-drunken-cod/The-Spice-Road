import { CLIMATE_COLORS, SEASONS, SLIDER_MAX, SLIDER_MIN, SLIDER_STEP, TIER_TARGETS } from "./config";
import type { AxisMeta } from "./data";
import { h, signed } from "./dom";
import { titleCase, type Entry, type Group, type Labels } from "./entries";
import { potency, roundToDecimals, scaleToPotency } from "./potency";
import type { Store } from "./store";

/** Everything a view needs to render and react to changes. */
export interface ViewContext {
  store: Store;
  labels: Labels;
  axes: AxisMeta[];
  /** Entries by path. */
  entries: Map<string, Entry>;
}

/** Collects update callbacks of rendered views, run when a profile changes. */
export class Bindings {
  private readonly byPath = new Map<string, Array<() => void>>();
  private readonly global: Array<() => void> = [];

  /**
   * Registers an update callback and runs it once.
   * @param path   The profile it depends on, or {@code null} to run on every change.
   * @param update The callback.
   */
  add(path: string | null, update: () => void): void {
    if (path === null)
      this.global.push(update);
    else
      this.byPath.set(path, [...(this.byPath.get(path) ?? []), update]);
    update();
  }

  /** @param path The profile that changed. */
  run(path: string): void {
    this.byPath.get(path)?.forEach(update => update());
    this.global.forEach(update => update());
  }

  /** Forgets every callback, before the views are rendered anew. */
  clear(): void {
    this.byPath.clear();
    this.global.length = 0;
  }
}

// #region axis control

/**
 * A slider and number input for one axis of one profile, colored by its poles.
 * @param ctx      The view context.
 * @param bindings Where to register updates.
 * @param path     The profile's path.
 * @param axis     The axis.
 * @param withPoles Whether to show the pole names on either side.
 * @return The control's element.
 */
export function axisControl(ctx: ViewContext, bindings: Bindings, path: string, axis: AxisMeta, withPoles: boolean): HTMLElement {
  const { store, labels } = ctx;
  const range = SLIDER_MAX - SLIDER_MIN;

  const slider = h("input", {
    type: "range",
    min: String(SLIDER_MIN),
    max: String(SLIDER_MAX),
    step: String(SLIDER_STEP),
    title: `${labels.pole(axis.id, false)} / ${labels.pole(axis.id, true)}`,
    on: { input: () => store.setAxis(path, axis.id, Number(slider.value)) },
  });
  const number = h("input", {
    type: "number",
    class: "axis-number",
    step: String(SLIDER_STEP),
    on: {
      input: () => {
        const value = Number(number.value);
        if (number.value.trim() !== "" && Number.isFinite(value))
          store.setAxis(path, axis.id, value);
      },
      blur: () => update(),
    },
  });
  const tick = h("span", { class: "default-tick" });

  const row = h("div", {
      class: withPoles ? "axis-row" : "axis-row compact",
      style: { "--neg": axis.negative_color, "--pos": axis.positive_color },
    },
    withPoles && h("span", { class: "pole neg" }, labels.pole(axis.id, false)),
    h("div", { class: "slider-wrap" }, slider, tick),
    withPoles && h("span", { class: "pole pos" }, labels.pole(axis.id, true)),
    number,
  );

  const update = (): void => {
    const value = store.get(path)[axis.id] ?? 0;
    const fallback = store.getDefault(path)[axis.id] ?? 0;
    slider.value = String(value);
    if (document.activeElement !== number || Number(number.value) !== value)
      number.value = String(roundToDecimals(value, 4));
    number.classList.toggle("out-of-range", value < SLIDER_MIN || value > SLIDER_MAX);
    row.classList.toggle("changed", Math.abs(value - fallback) > 1e-9);
    tick.style.setProperty("--at", String(Math.min(1, Math.max(0, (fallback - SLIDER_MIN) / range))));
    tick.title = `Default: ${signed(fallback)}`;
  };
  bindings.add(path, update);
  return row;
}

// #region actions

/**
 * @param ctx  The view context.
 * @param path A profile's path.
 * @return The Potency target of its Tier, or {@code null} if it has none.
 */
function targetOf(ctx: ViewContext, path: string): number | null {
  const tier = ctx.entries.get(path)?.tier;
  return tier ? TIER_TARGETS[tier] ?? null : null;
}

/**
 * Scales a profile to its Tier's target Potency.
 * @param ctx  The view context.
 * @param path The profile's path.
 */
function scaleEntry(ctx: ViewContext, path: string): void {
  const target = targetOf(ctx, path);
  if (target !== null)
    ctx.store.setProfile(path, scaleToPotency(ctx.store.get(path), target, SLIDER_STEP));
}

/**
 * @param ctx      The view context.
 * @param bindings Where to register updates.
 * @param path     The profile's path.
 * @return The Potency readout plus scale and reset buttons.
 */
function profileActions(ctx: ViewContext, bindings: Bindings, path: string): HTMLElement {
  const target = targetOf(ctx, path);
  const value = h("span", { class: "potency-value" });
  const reset = h("button", {
    type: "button",
    textContent: "Reset",
    title: "Reset to the baseline values",
    on: { click: () => ctx.store.reset(path) },
  });
  const scale = target !== null && h("button", {
    type: "button",
    textContent: "Scale",
    title: `Scale every axis to reach the Tier's target Potency of ${target.toFixed(2)}`,
    on: { click: () => scaleEntry(ctx, path) },
  });

  const el = h("div", { class: "actions" },
    h("span", { class: "potency", title: "Potency: sum of absolute axis values" }, "P ", value,
      target !== null && h("span", { class: "target" }, ` / ${target.toFixed(1)}`)),
    scale,
    reset,
  );

  bindings.add(path, () => {
    const current = potency(ctx.store.get(path));
    value.textContent = current.toFixed(2);
    value.classList.toggle("over", target !== null && current > target + SLIDER_STEP * 2);
    value.classList.toggle("under", target !== null && current < target - SLIDER_STEP * 2);
    reset.disabled = !ctx.store.isChanged(path);
  });
  return el;
}

// #region metadata

/**
 * @param entry The entry to describe.
 * @return Badges for difficulty, climate, seasons and growth.
 */
function metaBadges(entry: Entry): HTMLElement {
  const meta = entry.meta;
  if (!meta)
    return h("div", { class: "meta muted" }, "No spice metadata");

  const dots = h("span", { class: "difficulty", title: `Harvest difficulty ${meta.harvest_difficulty}/5` },
    ...Array.from({ length: 5 }, (_, i) => h("span", { class: i < meta.harvest_difficulty ? "dot on" : "dot" })));
  const seasons = h("span", { class: "seasons", title: "Growing seasons" },
    ...SEASONS.map(season => h("span", { class: meta.seasons.includes(season.id) ? "season on" : "season" }, season.label)));
  const flags = [
    meta.aquatic && "aquatic",
    meta.hand_pick && "hand-pick",
  ].filter(Boolean).join(", ");

  return h("div", { class: "meta" },
    dots,
    h("span", { class: "climate", style: { "--climate": CLIMATE_COLORS[meta.climate] ?? "#888" } }, titleCase(meta.climate)),
    seasons,
    h("span", { class: "growth muted", title: "Source type / harvest action" },
      `${titleCase(meta.source_type)} · ${titleCase(meta.harvest_action)}${flags ? ` · ${flags}` : ""}`),
  );
}

// #region card

/**
 * @param ctx      The view context.
 * @param bindings Where to register updates.
 * @param entry    The profile to show.
 * @return The profile's card in the list.
 */
export function card(ctx: ViewContext, bindings: Bindings, entry: Entry): HTMLElement {
  const { store, labels } = ctx;
  const checkbox = h("input", {
    type: "checkbox",
    class: "select",
    title: "Compare",
    on: { change: () => store.setSelected(entry.path, checkbox.checked) },
  });
  const note = h("textarea", {
    class: "note",
    rows: 1,
    placeholder: "Real-life flavor notes…",
    value: store.getNote(entry.path),
    on: { input: () => store.setNote(entry.path, note.value) },
  });

  const el = h("article", { class: "card", style: { "--tier-color": entry.tierColor } },
    h("header", {},
      checkbox,
      h("div", { class: "title" },
        h("span", { class: "name" }, entry.name),
        entry.meta?.processing && h("span", { class: "badge" }, labels.processing(entry.meta.processing)),
        h("span", { class: "changed-dot", title: "Changed" }),
        h("code", { class: "item-id" }, entry.target),
      ),
      profileActions(ctx, bindings, entry.path),
    ),
    metaBadges(entry),
    h("div", { class: "axes" }, ...ctx.axes.map(axis => axisControl(ctx, bindings, entry.path, axis, true))),
    note,
  );
  el.dataset.path = entry.path;
  el.dataset.search = `${entry.name} ${entry.target}`.toLowerCase();

  bindings.add(entry.path, () => el.classList.toggle("is-changed", store.isChanged(entry.path)));
  return el;
}

/**
 * Syncs every card's compare checkbox with the store's selection.
 * @param root  The element holding the cards.
 * @param store The store.
 */
export function syncCheckboxes(root: HTMLElement, store: Store): void {
  const selected = store.getSelected();
  root.querySelectorAll<HTMLElement>(".card").forEach(cardEl => {
    const path = cardEl.dataset.path!;
    const checkbox = cardEl.querySelector<HTMLInputElement>("input.select")!;
    checkbox.checked = selected.includes(path);
    checkbox.disabled = store.isSelectionFull(path);
    cardEl.classList.toggle("is-selected", checkbox.checked);
  });
}

// #region group

/**
 * @param ctx      The view context.
 * @param bindings Where to register updates.
 * @param group    The rarity group.
 * @return The group's section, with a header showing Potency statistics.
 */
export function groupSection(ctx: ViewContext, bindings: Bindings, group: Group): HTMLElement {
  const target = group.tier ? TIER_TARGETS[group.tier] ?? null : null;
  const stats = h("span", { class: "stats" });
  const scaleAll = target !== null && h("button", {
    type: "button",
    textContent: "Scale group",
    on: {
      click: () => {
        if (confirm(`Scale all ${group.entries.length} ${group.label} profiles to Potency ${target.toFixed(2)}?`))
          group.entries.forEach(entry => scaleEntry(ctx, entry.path));
      },
    },
  });

  const el = h("section", { class: "group", style: { "--tier-color": group.color } },
    h("header", { class: "group-header" },
      h("h2", {}, group.label),
      h("span", { class: "count muted" }, `${group.entries.length}`),
      stats,
      target !== null && h("span", { class: "muted" }, `target ${target.toFixed(1)}`),
      scaleAll,
    ),
    h("div", { class: "cards" }, ...group.entries.map(entry => card(ctx, bindings, entry))),
  );

  bindings.add(null, () => {
    const values = group.entries.map(entry => potency(ctx.store.get(entry.path)));
    const avg = values.reduce((sum, value) => sum + value, 0) / values.length;
    stats.textContent = `P min ${Math.min(...values).toFixed(2)} · avg ${avg.toFixed(2)} · max ${Math.max(...values).toFixed(2)}`;
  });
  return el;
}

// #region compare

/**
 * @param ctx      The view context.
 * @param bindings Where to register updates.
 * @return The compare panel for the selected profiles, or {@code null} if none are selected.
 */
export function comparePanel(ctx: ViewContext, bindings: Bindings): HTMLElement | null {
  const { store, labels } = ctx;
  const paths = store.getSelected().filter(path => ctx.entries.has(path));
  if (paths.length === 0)
    return null;

  const grid = h("div", { class: "compare-grid", style: { "--columns": String(paths.length) } });
  grid.append(h("div", { class: "corner" }));
  for (const path of paths) {
    const entry = ctx.entries.get(path)!;
    grid.append(h("div", { class: "compare-head", style: { "--tier-color": entry.tierColor } },
      h("div", { class: "title" },
        h("span", { class: "name" }, entry.name),
        entry.meta?.processing && h("span", { class: "badge" }, labels.processing(entry.meta.processing)),
        h("button", {
          type: "button",
          class: "remove",
          textContent: "×",
          title: "Remove from comparison",
          on: { click: () => store.setSelected(path, false) },
        }),
      ),
      entry.tier && h("div", { class: "tier-name" }, labels.tier(entry.tier)),
      metaBadges(entry),
      profileActions(ctx, bindings, path),
    ));
  }

  for (const axis of ctx.axes) {
    grid.append(h("div", { class: "compare-axis", style: { "--neg": axis.negative_color, "--pos": axis.positive_color } },
      h("span", { class: "pole neg" }, `← ${labels.pole(axis.id, false)}`),
      h("span", { class: "pole pos" }, `${labels.pole(axis.id, true)} →`),
    ));
    for (const path of paths)
      grid.append(axisControl(ctx, bindings, path, axis, false));
  }

  return h("aside", { class: "compare" },
    h("header", { class: "compare-title" },
      h("h2", {}, "Compare"),
      h("button", { type: "button", textContent: "Clear", on: { click: () => paths.forEach(path => store.setSelected(path, false)) } }),
    ),
    grid,
  );
}

import { loadLang, loadMeta, loadProfilesFromFolder, loadRepoProfiles } from "./data";
import { append, h } from "./dom";
import { buildGroups, Labels, resolveAxes, type Entry, type Group } from "./entries";
import { downloadChangedZip } from "./export";
import { Store } from "./store";
import { Bindings, comparePanel, groupSection, syncCheckboxes, type ViewContext } from "./views";

const meta = loadMeta();
const labels = new Labels(loadLang());
const repoFiles = loadRepoProfiles();
const axes = resolveAxes(meta, repoFiles);
const store = new Store(axes.map(axis => axis.id), repoFiles);

const listBindings = new Bindings();
const compareBindings = new Bindings();
const toolbarBindings = new Bindings();

// #region shell

const listEl = h("main", { class: "list" });
const compareSlot = h("div", { class: "compare-slot" });
const search = h("input", {
  type: "search",
  placeholder: "Filter spices…",
  on: { input: () => applyFilter() },
});
const folderInput = h("input", {
  type: "file",
  hidden: true,
  webkitdirectory: true,
  multiple: true,
  on: { change: () => void loadFolder() },
});
const baselineInfo = h("span", { class: "baseline muted" });
const repoButton = h("button", {
  type: "button",
  textContent: "Use repo files",
  title: "Switch the baseline back to the profiles in the repository",
  on: { click: () => store.useRepoBaseline() },
});
const exportButton = h("button", {
  type: "button",
  class: "primary",
  on: { click: () => exportZip() },
});
const discardButton = h("button", {
  type: "button",
  textContent: "Discard all",
  on: {
    click: () => {
      if (confirm(`Discard all ${store.changedPaths().length} changed profiles?`))
        store.resetAll();
    },
  },
});
const status = h("span", { class: "status muted", role: "status" });

const app = document.querySelector<HTMLElement>("#app")!;
append(app,
  h("header", { class: "toolbar" },
    h("h1", {}, "Spice Balancer"),
    search,
    baselineInfo,
    repoButton,
    h("button", {
      type: "button",
      textContent: "Load folder…",
      title: "Use a folder of spice profile JSONs (e.g. a datapack) as the baseline",
      on: { click: () => folderInput.click() },
    }),
    folderInput,
    status,
    discardButton,
    exportButton,
  ),
  !meta && h("div", { class: "warning" },
    "dev/spice_meta.json is missing, so tiers, climates and seasons are unknown. Run datagen and reload."),
  h("div", { class: "layout" }, listEl, compareSlot),
);

// #region rendering

/** @return The rarity groups and view context for the current baseline. */
function context(): { groups: Group[]; ctx: ViewContext } {
  const groups = buildGroups(store.files(), meta, labels);
  const entries = new Map<string, Entry>(groups.flatMap(group => group.entries.map(entry => [entry.path, entry])));
  return { groups, ctx: { store, labels, axes, entries } };
}

let ctx: ViewContext;

/** Renders the whole list and compare panel anew, e.g. after the baseline changed. */
function renderAll(): void {
  const built = context();
  ctx = built.ctx;
  listBindings.clear();
  listEl.replaceChildren(...built.groups.map(group => groupSection(ctx, listBindings, group)));
  renderCompare();
  applyFilter();
  toolbarBindings.run("");
}

/** Renders the compare panel anew and syncs the checkboxes with the selection. */
function renderCompare(): void {
  compareBindings.clear();
  const panel = comparePanel(ctx, compareBindings);
  compareSlot.replaceChildren(...(panel ? [panel] : []));
  compareSlot.classList.toggle("open", panel !== null);
  syncCheckboxes(listEl, store);
}

/** Hides cards that don't match the filter, and groups left empty by it. */
function applyFilter(): void {
  const query = search.value.trim().toLowerCase();
  listEl.querySelectorAll<HTMLElement>(".group").forEach(group => {
    let visible = 0;
    group.querySelectorAll<HTMLElement>(".card").forEach(cardEl => {
      const match = !query || cardEl.dataset.search!.includes(query);
      cardEl.hidden = !match;
      if (match)
        visible++;
    });
    group.hidden = visible === 0;
  });
}

toolbarBindings.add(null, () => {
  const changed = store.changedPaths().length;
  exportButton.textContent = `Export ZIP (${changed})`;
  exportButton.disabled = changed === 0;
  discardButton.disabled = changed === 0;
  const name = store.getBaselineName();
  baselineInfo.textContent = `${store.files().length} profiles · baseline: ${name ?? "repository"}`;
  repoButton.hidden = name === null;
});

// #region actions

/** Loads the picked folder as the new baseline. */
async function loadFolder(): Promise<void> {
  const fileList = folderInput.files;
  if (!fileList?.length)
    return;

  const { files, skipped } = await loadProfilesFromFolder(fileList);
  folderInput.value = "";
  if (files.length === 0) {
    status.textContent = "No spice profiles found in that folder.";
    return;
  }

  const name = fileList[0]!.webkitRelativePath.split("/")[0] || "folder";
  store.setBaseline(files, name);
  status.textContent = `Loaded ${files.length} profiles${skipped ? `, skipped ${skipped} invalid JSON files` : ""}.`;
}

/** Downloads the changed profiles and reports how many there were. */
function exportZip(): void {
  const count = downloadChangedZip(store);
  status.textContent = count ? `Exported ${count} profiles.` : "Nothing changed.";
}

// #region wiring

store.onProfile(path => {
  listBindings.run(path);
  compareBindings.run(path);
  toolbarBindings.run(path);
});
store.onStructure(renderAll);
store.onSelection(renderCompare);

renderAll();

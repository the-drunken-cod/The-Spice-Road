import { fileURLToPath } from "node:url";
import { defineConfig } from "vite";

/** Repository root, which the tool reads profiles, spice meta and lang files from. */
const repoRoot = fileURLToPath(new URL("../..", import.meta.url));

export default defineConfig({
  server: {
    open: true,
    fs: {
      allow: [repoRoot],
    },
  },
});

import { readFileSync, writeFileSync } from "node:fs";
import { createTable, defaultTableLineCharset, type TableColumnAlign } from "@sv443-network/coreutils";

// #region args
/**
 * Aligns every column of a CSV file so its values line up vertically when viewed in a monospace font.
 * Numeric columns are right-aligned, all other columns are left-aligned.
 * @example
 * ```
 * pnpm run pad_csv -- ./in.csv ./out.csv
 * ```
 */

/** Extra padding added to the left and right of number cell contents */
const extraNumberPaddingAmount = 1;

const [, , inputPath, outputPath] = process.argv;

if(!inputPath || !outputPath) {
  console.error("Usage: pad_csv <input.csv> <output.csv>");
  process.exit(1);
}

// #region parse

const cPad = " ".repeat(extraNumberPaddingAmount);
const rows = readFileSync(inputPath, "utf8")
  .split(/\r?\n/)
  .filter((line) => line.length > 0)
  .map((line) => line.split(",")
    .map((cell) => Boolean(cell) && !isNaN(Number(cell)) ? `${cPad}${cell}${cPad}` : cell)
  );

const colCount = rows[0]?.length ?? 0;

/** Whether every data row (i.e. excluding the header) has a numeric value in the given column. */
function isNumericColumn(colIdx: number): boolean {
  return rows.slice(1).every((row) => {
    const cell = row[colIdx]?.trim();
    return Boolean(cell) && !isNaN(Number(cell));
  });
}

const columnAlign: TableColumnAlign[] = Array.from(
  { length: colCount },
  (_, colIdx) => isNumericColumn(colIdx) ? "right" : "left",
);

// #region render

const table = createTable(rows, {
  columnAlign,
  minPadding: 0,
  lineStyle: "none",
  lineCharset: {
    ...defaultTableLineCharset,
    none: { ...defaultTableLineCharset.none, vertical: "," },
  },
});

const padded = table
  .split("\n")
  .map((line) => line.slice(1, -1))
  .join("\n");

writeFileSync(outputPath, `${padded}\n`, "utf8");

console.log(`Padded CSV written to ${outputPath}`);

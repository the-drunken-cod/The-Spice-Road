import { readFile, constants as fsconst } from "node:fs/promises";
import { dirname, join } from "node:path";
import { styleText } from "node:util";
import { scheduleExit } from "@sv443-network/coreutils";
import { fileURLToPath } from "node:url";

const addNewline = true;

const __dirname = dirname(fileURLToPath(import.meta.url));

const fileMapping = {
  "gradle.properties": [getPath("./gradle.properties"), resolvePropertiesValue],
  "1.21.1": [getPath("./versions/1.21.1.properties"), resolvePropertiesValue],
} as const satisfies Record<string, [
  path: string,
  resolveValue: (path: string, key: string) => Promise<string | undefined> | string | undefined,
]>;

function getPath(projectRootRelativePath: string) {
  return join(__dirname, "../../", projectRootRelativePath);
}

async function resolvePropertiesValue(path: string, key: string): Promise<string | undefined> {
  let content: string | undefined;
  try {
    content = String(await readFile(path, { encoding: "utf-8", flag: fsconst.O_RDONLY }));
  }
  catch(err) {
    console.error(styleText("red", `Error while reading file at path '${path}':`), err);
    return;
  }

  const execRes = new RegExp(`^\\s*${key}=(.+)\s*$`, "m").exec(content);

  if(typeof execRes?.[1] === "string")
    return execRes[1];
  else
    console.error(styleText("red", `Couldn't find key '${key}' in file '${path}'`));
    return;
}

async function run() {
  const fileName = process.argv[2] as keyof typeof fileMapping;
  const key = process.argv[3];

  if(fileName in fileMapping) {
    const [path, resolveValue] = fileMapping[fileName];

    const value = await resolveValue(path, key);

    if(value) {
      if(addNewline)
        console.log(value);
      else
        process.stdout.write(value);
      return scheduleExit(0);
    }
    else
      return scheduleExit(1);
  }

  console.error(styleText("red", `File '${fileName}' is not defined in 'fileMapping' in './tools/extract_props/index.ts'!`));
  return scheduleExit(1);
}

run();

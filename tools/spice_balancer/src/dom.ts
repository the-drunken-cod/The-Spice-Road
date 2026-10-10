/** Anything that can be passed as a child to {@link h}. Falsy values are skipped. */
export type Child = Node | string | number | null | undefined | false;

/** Attributes and properties accepted by {@link h}. */
export interface Props {
  class?: string;
  /** CSS properties and custom properties, e.g. {@code { "--tier-color": "#fff" }}. */
  style?: Record<string, string>;
  /** Event listeners, keyed by event name. */
  on?: Partial<Record<keyof HTMLElementEventMap, (event: Event) => void>>;
  /** Any other property, assigned directly to the element. */
  [property: string]: unknown;
}

/**
 * Creates an element.
 * @param tag      The tag name.
 * @param props    Class, style, listeners and properties to set.
 * @param children Child nodes or text.
 * @return The new element.
 */
export function h<K extends keyof HTMLElementTagNameMap>(tag: K, props: Props = {}, ...children: Child[]): HTMLElementTagNameMap[K] {
  const el = document.createElement(tag);
  for (const [key, value] of Object.entries(props)) {
    if (value === undefined)
      continue;
    if (key === "class")
      el.className = value as string;
    else if (key === "style")
      Object.entries(value as Record<string, string>).forEach(([name, val]) => el.style.setProperty(name, val));
    else if (key === "on")
      Object.entries(value as Record<string, (event: Event) => void>).forEach(([name, listener]) => el.addEventListener(name, listener));
    else
      Reflect.set(el, key, value);
  }
  append(el, ...children);
  return el;
}

/**
 * @param parent   The element to append to.
 * @param children Child nodes or text; falsy values are skipped.
 */
export function append(parent: Element, ...children: Child[]): void {
  for (const child of children) {
    if (child === null || child === undefined || child === false)
      continue;
    parent.append(child instanceof Node ? child : String(child));
  }
}

/**
 * @param value A number.
 * @param decimals Decimals to show.
 * @return The number with a sign, e.g. {@code +0.80} or {@code -0.30}.
 */
export function signed(value: number, decimals = 2): string {
  return (value > 0 ? "+" : value < 0 ? "" : "±") + value.toFixed(decimals);
}

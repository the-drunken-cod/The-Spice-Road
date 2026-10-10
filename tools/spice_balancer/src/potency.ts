import type { Profile } from "./data";

/** Values closer together than this count as equal. */
const EPSILON = 1e-9;

/**
 * @param profile A profile.
 * @return Its Potency: the sum of the absolute values of all axes.
 */
export function potency(profile: Profile): number {
  return Object.values(profile).reduce((sum, value) => sum + Math.abs(value), 0);
}

/**
 * @param value The value to round.
 * @param step  The step to round to, e.g. {@code 0.05}.
 * @return {@code value} rounded to the closest multiple of {@code step}, without float noise.
 */
export function roundToStep(value: number, step: number): number {
  const decimals = Math.max(0, Math.ceil(-Math.log10(step)) + 1);
  return Number((Math.round(value / step) * step).toFixed(decimals));
}

/**
 * @param value    The value to round.
 * @param decimals The decimals to keep.
 * @return {@code value} rounded to {@code decimals} decimals.
 */
export function roundToDecimals(value: number, decimals: number): number {
  return Number(value.toFixed(decimals));
}

/**
 * Multiplies every axis by the same factor so the profile keeps its shape but
 * lands on {@code target} Potency. Each axis is rounded to {@code step}, and
 * the rounding is spread across the axes with the largest remainders, so the
 * result hits the target as closely as the step allows.
 * @param profile The profile to scale.
 * @param target  The Potency to aim for.
 * @param step    The step to round each axis to.
 * @return The scaled profile, or a copy of {@code profile} if it has no Potency to scale.
 */
export function scaleToPotency(profile: Profile, target: number, step: number): Profile {
  const current = potency(profile);
  if (current < EPSILON)
    return { ...profile };

  const factor = target / current;
  const exact = Object.entries(profile).map(([axis, value]) => {
    const steps = Math.abs(value * factor) / step;
    return { axis, sign: Math.sign(value), steps: Math.floor(steps + EPSILON), remainder: steps - Math.floor(steps + EPSILON) };
  });

  let missing = Math.round(target / step) - exact.reduce((sum, entry) => sum + entry.steps, 0);
  for (const entry of [...exact].sort((a, b) => b.remainder - a.remainder)) {
    if (missing <= 0)
      break;
    if (entry.sign !== 0) {
      entry.steps++;
      missing--;
    }
  }

  const scaled: Profile = {};
  for (const entry of exact)
    scaled[entry.axis] = roundToStep(entry.sign * entry.steps * step, step);
  return scaled;
}

/**
 * @param a     A profile.
 * @param b     Another profile.
 * @param axes  The axes to compare; missing axes count as 0.
 * @return Whether both profiles have the same value on every axis.
 */
export function profilesEqual(a: Profile, b: Profile, axes: readonly string[]): boolean {
  return axes.every(axis => Math.abs((a[axis] ?? 0) - (b[axis] ?? 0)) < EPSILON);
}

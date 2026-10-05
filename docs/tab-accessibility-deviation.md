# Persistent tab accessibility deviation

The native persistent bottom navigation deliberately does **not** reproduce the
clipped 320 px Tamil HTML geometry.

## Reference evidence

Run #131 reference metrics on commit
`9d783cf75c64425a216b85954679caacb5eb7786` show the six Tamil HTML tab boxes
at 320 px as:

| Tab | Left px | Width px | Right px |
|---|---:|---:|---:|
| Home | -17.5 | 54 | 36.5 |
| Learn | 36.5 | 42 | 78.5 |
| Elders | 78.5 | 71 | 149.5 |
| Class | 149.5 | 56 | 205.5 |
| Council | 205.5 | 69 | 274.5 |
| Predict | 274.5 | 63 | 337.5 |

The intrinsic tab widths total **355 px**. The CSS tab bar has 6 px padding on
both sides, leaving **308 px** of usable width in a 320 px viewport. The web
reference therefore extends from **-17.5 px to 337.5 px** and clips at both
viewport edges.

That clipping is reference evidence, not an accessibility target.

## Native contract

The native implementation uses six equal proportional columns inside the
available tab-bar width:

- 320 dp: `(320 - 12) / 6 = 51.333... dp` per tab;
- 390 dp: `(390 - 12) / 6 = 63 dp` per tab.

Every interactive tab semantics/touch box must be at least **48 x 48 dp**.

To keep all persistent navigation labels fully visible at both 100% and 200%
font scale, the tab-label visual size is bounded independently from the rest of
the app typography:

- English: effective 12 px-equivalent label size;
- Tamil at <=360 dp: effective 8.5 px-equivalent label size;
- Tamil above 360 dp: effective 10.5 px-equivalent label size.

The implementation counter-scales only the persistent tab labels against
`fontScale`; all ordinary content continues to honour app/system font scaling.
This is an explicit accessibility exception for a fixed six-item navigation
bar.

## HARD gate

The HARD tab gate does not require line-count parity with the clipped web
reference. Reference/native tab line-count differences are informational.

HARD requires, at 320 and 390 dp and at 100% and 200% font scale:

1. all six tab labels have zero width and height overflow;
2. all six visible tab boxes remain inside the viewport;
3. all six semantic touch targets are at least 48 x 48 dp.

No PARITY baseline or threshold is changed by this deviation.

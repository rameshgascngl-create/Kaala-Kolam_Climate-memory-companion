# T12 Tamil screenshot gate

T12 adds a separate **Tamil-only 80-screen localisation stress matrix** without declaring the visual redesign complete.

Matrix:
- screens: Home, Learn, Elders, Class, Council, Predict, Learn topic, Learn deep dive, game, word list;
- viewports: 390×844 and 320×568;
- font scale: 100% and 200%;
- themes: dark and light;
- language: Tamil.

Playwright captures the audited HTML at the same viewport/theme/state and forces the root font size to 100% or 200%. Paparazzi captures the native Compose fixture with corresponding font scale. `tools/compare_tamil_visuals.py` records measured MAE and changed-pixel percentages for every screen. Pixel FAIL is expected while placeholder workflows remain; this task does not relabel that visual-fidelity work as complete.

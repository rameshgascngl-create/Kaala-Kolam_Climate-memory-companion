# Tamil font fallback

T6 bundles Noto Sans Tamil Regular under SIL OFL 1.1 as a fallback only. Android `Paint.hasGlyph` checks the system sans-serif against the Tamil probe at runtime; the app uses the bundled font when system coverage is incomplete.

Bundled font source: notofonts/noto-fonts `hinted/ttf/NotoSansTamil/NotoSansTamil-Regular.ttf`.
Raw TTF size: **74,248 bytes**. This is the deterministic raw-resource cost before APK compression/alignment. Final APK delta is reported separately only when a build from this exact commit is available.

Recommendation: keep the 74,248-byte fallback because it protects Tamil rendering on devices whose system font lacks required glyphs.

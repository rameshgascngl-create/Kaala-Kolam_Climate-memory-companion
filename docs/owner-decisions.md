# Owner decisions

## Tamil terminology pending review

The following two source strings retain **குளிர்ச்சிக் காப்பகங்கள்** exactly as requested. They describe heat/cooling shelters, not cyclone evacuation shelters. The native app may ship these source words only while they remain explicitly allow-listed by path; a future wording change requires owner review rather than machine translation.

1. `topics[23].a.ta` — adaptation explanation: `நிழல் மற்றும் குளிர்ச்சிக் காப்பகங்கள்`
2. `clips.act.steps[2].c.ta` — adaptation clip caption: `நிழல் மற்றும் குளிர்ச்சிக் காப்பகங்கள்`

Decision needed: choose the preferred Tamil wording for “cooling shelters” and then remove these two allow rules.

## Refrigerator terminology

`குளிர்சாதனப் பெட்டி` / `குளிர்சாதனப் பெட்டிகள்` is explicitly allowed when it means refrigerator(s). The banned term policy applies to **குளிர்சாதனம்** when used for an air conditioner; the approved air-conditioner term remains **குளிரூட்டி**.

## Elders story-length rule

The audited web prototype uses `maxlength="400"` on the elder-story textarea, which counts browser string/code-unit length. For native Elders Slice C, the owner-approved rule is **500 grapheme clusters** instead.

The native app therefore uses `BreakIterator.getCharacterInstance(...)` to count and limit the story field. Ordinary IME updates preserve the complete `TextFieldValue`, including its composing region. When an imported or restored backup contains a story beyond 500 grapheme clusters, validation trims only at a BreakIterator boundary; it must never cut the UTF-16 string with `take()` or an arbitrary code-unit boundary. This is an intentional native deviation from the v1.0 HTML reference.


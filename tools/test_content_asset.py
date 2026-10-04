#!/usr/bin/env python3
from __future__ import annotations

import json
import unittest
from pathlib import Path

ASSET_DIR = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "assets" / "content"
EXPECTED = {
    "topics": 24,
    "clips": 24,
    "words": 77,
    "elderQuestions": 10,
    "councilOptions": 7,
    "councilGroups": 4,
    "councilScenarios": 3,
    "councilRoles": 6,
    "predictions": 8,
    "demos": 3,
    "games": 2,
}
BANNED = [
    "ஆல்கா", "கார்பன் டையாக்சைடு", "தரைக் காற்று", "கடல் காற்று",
    "பனிப்பாறை", "குளிர்சாதனம்", "சதுப்புநிலக் காடுகள்", "தழுவல்",
    "அழுத்தம்", "காப்பகம்",
]


def present(value):
    if isinstance(value, str):
        return bool(value.strip())
    if isinstance(value, (list, dict)):
        return bool(value)
    return value is not None


def bilingual_errors(value, path="$"):
    errors = []
    if isinstance(value, dict):
        if "en" in value or "ta" in value:
            if not present(value.get("en")):
                errors.append(path + ": English missing/empty")
            if not present(value.get("ta")):
                errors.append(path + ": Tamil missing/empty")
        for key, child in value.items():
            errors.extend(bilingual_errors(child, f"{path}.{key}"))
    elif isinstance(value, list):
        for index, child in enumerate(value):
            errors.extend(bilingual_errors(child, f"{path}[{index}]"))
    return errors


class ContentAssetTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.texts = {p.name: p.read_text(encoding="utf-8") for p in sorted(ASSET_DIR.glob("*.json"))}
        cls.payloads = {name: json.loads(text) for name, text in cls.texts.items()}
        cls.all_text = "".join(cls.texts.values())
        cls.all_clips = {}
        for i in range(1, 5):
            cls.all_clips.update(cls.payloads[f"clips-{i}.json"]["clips"])
        cls.topics = cls.payloads["topics-1.json"]["topics"] + cls.payloads["topics-2.json"]["topics"]

    def test_counts(self):
        self.assertEqual(EXPECTED, self.payloads["manifest.json"]["counts"])
        self.assertEqual(24, len(self.topics))
        self.assertEqual(24, len(self.all_clips))
        self.assertEqual([6, 6, 6, 6], [len(self.payloads[f"clips-{i}.json"]["clips"]) for i in range(1, 5)])
        self.assertEqual(77, len(self.payloads["words.json"]["words"]))
        self.assertEqual(10, len(self.payloads["elders.json"]["elderQuestions"]))
        self.assertEqual(8, len(self.payloads["predictions.json"]["predictions"]))
        council = self.payloads["council.json"]
        self.assertEqual((7, 4, 3, 6), (len(council["options"]), len(council["groups"]), len(council["scenarios"]), len(council["roles"])))
        self.assertEqual(3, len(self.payloads["shared.json"]["data"]["demoIds"]))
        self.assertEqual(2, len(self.payloads["games.json"]["games"]))

    def test_every_bilingual_object_has_both_languages(self):
        merged = {name: payload for name, payload in self.payloads.items() if name != "manifest.json"}
        self.assertEqual([], bilingual_errors(merged))

    def test_banned_tamil_terms_absent(self):
        for term in BANNED:
            self.assertNotIn(term, self.all_text, term)

    def test_utf8_is_clean(self):
        self.assertNotIn("\ufffd", self.all_text)
        for marker in ("Ã", "Â", "â€", "ðŸ"):
            self.assertNotIn(marker, self.all_text)

    def test_word_draft_flags_preserved(self):
        words = self.payloads["words.json"]["words"]
        flags = [row[3] for row in words]
        self.assertTrue(all(flag in (0, 1) for flag in flags))
        self.assertEqual(8, flags.count(0))


if __name__ == "__main__":
    unittest.main(verbosity=2)

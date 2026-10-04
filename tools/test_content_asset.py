#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSET_DIR = ROOT / "app" / "src" / "main" / "assets" / "content"
OVERRIDE_FILE = ROOT / "tools" / "content_overrides.json"
DRAFT_FILE = ROOT / "docs" / "tamil-drafts-DRAFT.json"
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
EXPECTED_GAPS = {
    "deepDive": 0,
    "councilDescription": 0,
    "eldersCrossCheck": 0,
    "predictExplanation": 0,
}


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


def walk_strings(value, path=""):
    if isinstance(value, str):
        yield path, value
    elif isinstance(value, list):
        for index, child in enumerate(value):
            yield from walk_strings(child, f"{path}[{index}]")
    elif isinstance(value, dict):
        for key, child in value.items():
            child_path = f"{path}.{key}" if path else key
            yield from walk_strings(child, child_path)


def is_english_only(value):
    if isinstance(value, str):
        return bool(value.strip())
    if isinstance(value, dict):
        return present(value.get("en")) and not present(value.get("ta"))
    return False


def canonical_source_path(asset_name, path):
    if asset_name == "topics-2.json" and path.startswith("topics["):
        close = path.index("]")
        index = int(path[len("topics["):close]) + 12
        return f"topics[{index}]" + path[close + 1:]
    return path


class ContentAssetTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.texts = {p.name: p.read_text(encoding="utf-8") for p in sorted(ASSET_DIR.glob("*.json"))}
        cls.payloads = {name: json.loads(text) for name, text in cls.texts.items()}
        cls.all_text = "".join(cls.texts.values())
        cls.overrides = json.loads(OVERRIDE_FILE.read_text(encoding="utf-8"))
        cls.draft = json.loads(DRAFT_FILE.read_text(encoding="utf-8"))
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
        merged = {name: payload for name, payload in self.payloads.items() if name not in ("manifest.json", "gaps.json")}
        self.assertEqual([], bilingual_errors(merged))

    def test_banned_tamil_stems_absent_except_explicit_allow_rules(self):
        patterns = self.overrides.get("bannedPatterns", {})
        allow_rules = self.overrides.get("allowRules", [])
        self.assertEqual(set(self.overrides["bannedTerms"]), set(patterns))
        self.assertTrue(all("\\u0B80-\\u0BFF" in pattern for pattern in patterns.values()))

        allowed_hits = []
        violations = []
        for asset_name, payload in self.payloads.items():
            if asset_name in ("manifest.json", "gaps.json"):
                continue
            for path, text in walk_strings(payload):
                logical_path = canonical_source_path(asset_name, path)
                for term in self.overrides["bannedTerms"]:
                    pattern = re.compile(patterns[term])
                    if not pattern.search(text):
                        continue
                    matched_rule = next(
                        (
                            rule for rule in allow_rules
                            if rule.get("term") == term
                            and rule.get("path") == logical_path
                            and rule.get("exactValue") == text
                            and re.search(rule.get("pattern", patterns[term]), text)
                        ),
                        None,
                    )
                    if matched_rule:
                        allowed_hits.append((term, logical_path, text))
                    else:
                        violations.append((asset_name, logical_path, term, text))
        self.assertEqual([], violations)
        expected = sorted((rule["term"], rule["path"], rule["exactValue"]) for rule in allow_rules)
        self.assertEqual(expected, sorted(allowed_hits))

    def test_cyclone_shelter_inflections_use_safety_centre(self):
        cyclone_topic = self.topics[8]["k"]["ta"]
        cyclone_clip = self.all_clips["cyclone"]["steps"][5]["c"]["ta"]
        for text in (cyclone_topic, cyclone_clip):
            self.assertIn("பாதுகாப்பு மையத்துக்குச்", text)
            self.assertNotIn("காப்பகத்துக்குச்", text)

    def test_heat_shelter_wording_is_intentionally_pending_owner_decision(self):
        topic = self.topics[23]["a"]["ta"]
        clip = self.all_clips["act"]["steps"][2]["c"]["ta"]
        self.assertIn("குளிர்ச்சிக் காப்பகங்கள்", topic)
        self.assertIn("குளிர்ச்சிக் காப்பகங்கள்", clip)

    def test_refrigerator_wording_is_explicitly_allowed(self):
        self.assertIn("குளிர்சாதனப் பெட்டி", self.payloads["words.json"]["words"][53][2])
        self.assertIn("குளிர்சாதனப் பெட்டிகள்", self.all_clips["ozone"]["steps"][1]["c"]["ta"] )


    def test_long_form_gap_inventory_is_exact(self):
        actual = []
        for index, topic in enumerate(self.topics):
            if is_english_only(topic.get("d")):
                actual.append((f"topics[{index}].d", "deepDive"))
        council = self.payloads["council.json"]
        for index, option in enumerate(council["options"]):
            if is_english_only(option.get("de")):
                actual.append((f"council.options[{index}].de", "councilDescription"))
        elders = self.payloads["elders.json"]["elderQuestions"]
        for index, question in enumerate(elders):
            for field in ("sci", "conf", "check"):
                if is_english_only(question.get(field)):
                    actual.append((f"elders[{index}].{field}", "eldersCrossCheck"))
        predictions = self.payloads["predictions.json"]["predictions"]
        for index, prediction in enumerate(predictions):
            for field in ("truth", "why"):
                if is_english_only(prediction.get(field)):
                    actual.append((f"predictions[{index}].{field}", "predictExplanation"))

        declared_payload = self.payloads["gaps.json"]
        declared = [(item["logicalPath"], item["kind"]) for item in declared_payload["gaps"]]
        self.assertEqual(actual, declared)
        self.assertEqual([], actual)
        self.assertEqual([], declared)
        self.assertEqual(0, declared_payload["count"])
        self.assertEqual(EXPECTED_GAPS, declared_payload["countsByKind"])
        self.assertEqual("English only", declared_payload["uiLabel"])

    def test_merged_tamil_remains_owner_unreviewed_draft(self):
        self.assertEqual("DRAFT", self.draft["status"])
        self.assertTrue(self.draft["shippedWithApp"])
        self.assertEqual(77, self.draft["count"])
        self.assertEqual(77, len(self.draft["entries"]))
        self.assertTrue(all(item["taDraft"] for item in self.draft["entries"]))
        self.assertTrue(all(item["status"] == "DRAFT" for item in self.draft["entries"]))
        self.assertTrue(all(item["reviewState"] == "NEEDS_OWNER_REVIEW" for item in self.draft["entries"]))
        self.assertEqual({item["key"]: item["ta"] for item in self.overrides["draftTamil"]}, {item["key"]: item["taDraft"] for item in self.draft["entries"]})
        self.assertNotIn("tamil-drafts-DRAFT.json", self.texts)

    def test_teacher_role_uses_approved_safety_centre_term(self):
        roles = self.payloads["council.json"]["roles"]
        teacher = next(role for role in roles if role.get("en") == "School teacher")
        self.assertIn("பாதுகாப்பு மையம்", teacher["p"]["ta"])
        self.assertNotIn("காப்பகம்", teacher["p"]["ta"])

    def test_utf8_is_clean(self):
        combined = self.all_text + DRAFT_FILE.read_text(encoding="utf-8") + OVERRIDE_FILE.read_text(encoding="utf-8")
        self.assertNotIn("\ufffd", combined)
        for marker in ("Ã", "Â", "â€", "ðŸ"):
            self.assertNotIn(marker, combined)

    def test_word_draft_flags_preserved(self):
        words = self.payloads["words.json"]["words"]
        flags = [row[3] for row in words]
        self.assertTrue(all(flag in (0, 1) for flag in flags))
        self.assertEqual(8, flags.count(0))


if __name__ == "__main__":
    unittest.main(verbosity=2)

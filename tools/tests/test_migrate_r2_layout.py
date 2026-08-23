import importlib.util
import sys
import unittest
from pathlib import Path


SCRIPT_PATH = Path(__file__).parents[1] / "migrate_r2_layout.py"
SPEC = importlib.util.spec_from_file_location("migrate_r2_layout", SCRIPT_PATH)
assert SPEC is not None and SPEC.loader is not None
MODULE = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)


class MigrationLayoutTest(unittest.TestCase):
    def test_production_mapping_is_exact(self):
        mappings = MODULE.validate_mappings(MODULE.MAPPINGS)
        self.assertEqual(13, len(mappings))
        self.assertEqual("lectures/v1/lecture-01.pdf", mappings[0].target)
        self.assertEqual("lectures/v1/lecture-12.pdf", mappings[11].target)
        self.assertEqual("privacy-policy/v4.0/privacy-policy.pdf", mappings[12].target)

    def test_protected_prefix_is_rejected(self):
        mappings = list(MODULE.MAPPINGS)
        mappings[0] = MODULE.ObjectMapping(
            "lectures/Basic Algebraic Structures.pdf",
            "releases/v1/lecture-01.pdf",
        )
        with self.assertRaises(MODULE.MigrationError):
            MODULE.validate_mappings(mappings)

    def test_duplicate_target_is_rejected(self):
        mappings = list(MODULE.MAPPINGS)
        mappings[1] = MODULE.ObjectMapping(mappings[1].source, mappings[0].target)
        with self.assertRaises(MODULE.MigrationError):
            MODULE.validate_mappings(mappings)

    def test_path_traversal_is_rejected(self):
        mappings = list(MODULE.MAPPINGS)
        mappings[0] = MODULE.ObjectMapping(
            "lectures/../Basic Algebraic Structures.pdf",
            mappings[0].target,
        )
        with self.assertRaises(MODULE.MigrationError):
            MODULE.validate_mappings(mappings)


if __name__ == "__main__":
    unittest.main()

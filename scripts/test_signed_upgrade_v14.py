"""Guard against accidentally running the v14 signed upgrade on personal devices."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]


class SignedUpgradeV14HarnessTest(unittest.TestCase):
    def test_harness_requires_owned_fresh_android_emulator(self):
        source = (ROOT / "scripts/run-signed-upgrade-v14-qa.ps1").read_text(encoding="utf-8")
        for required in (
            "^emulator-\\d+$", "V14_REFUSE_ANY_OTHER_OR_PERSONAL_AVD",
            "V14_UPGRADE_REQUIRES_FRESH_AVD_NO_MUSICFLOW_APP",
            "MusicFlow_CommunityV14_QA_API33",
            "MusicFlow_CommunityV14_QA_API36",
            "V14_WRONG_QA_EMULATOR_API_OR_UNBOOTED",
            "V14_QA_APK_HASH_MISMATCH", "V14_QA_APK_DIFFERENT_SIGNERS",
        ):
            self.assertIn(required, source)
        executable_lines = [
            line for line in source.splitlines()
            if line.strip() and not line.lstrip().startswith("#")
        ]
        self.assertNotIn("pm clear", "\n".join(executable_lines).lower())
        self.assertNotIn("-wipe-data", source)

    def test_seed_and_verify_are_separated_by_actual_apk_replacement(self):
        source = (ROOT / "scripts/run-signed-upgrade-v14-qa.ps1").read_text(encoding="utf-8")
        seed = source.index("TestQa 'seedSignedV5PlaylistFavoriteAndRealSafGrant'")
        replaced = source.index("InstallQa $newApk", seed)
        verified = source.index("TestQa 'verifySignedV6PreservesAllDataAndCleanUpTestOwnedOnly'", replaced)
        self.assertLess(seed, replaced)
        self.assertLess(replaced, verified)
        self.assertIn("V14_SIGNED_UPGRADE_V13_TO_V14_ROOM_FAVORITES_SAF", source)


if __name__ == "__main__":
    unittest.main()

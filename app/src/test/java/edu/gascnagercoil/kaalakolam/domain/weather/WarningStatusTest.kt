package edu.gascnagercoil.kaalakolam.domain.weather

import org.junit.Assert.assertEquals
import org.junit.Test

class WarningStatusTest {
    private val scope = WarningScope(WarningAuthority.IMD, "test-district", "heavy-rain")
    private val baseline = WarningBulletin(scope, "https://api.imd.gov.in/test-fixture", 100L, 100L, 500L,
        BulletinFinding.Warning("synthetic severity", "synthetic message"))
    private fun status(bulletin: WarningBulletin?, now: Long = 200, age: Long = 300,
                       requested: WarningScope = scope) = WarningStatusPolicy.evaluate(requested, bulletin, now, age)
    private fun unknown(reason: WarningUnknownReason, actual: WarningStatus) =
        assertEquals(WarningStatus.Unknown(reason), actual)

    @Test fun missingNeverMeansNoWarning() = unknown(WarningUnknownReason.UNAVAILABLE, status(null))
    @Test fun currentOfficialWarningRetainsBulletin() = assertEquals(WarningStatus.ActiveWarning(baseline), status(baseline))
    @Test fun explicitNoWarningRequiresValidBulletin() {
        val clear = baseline.copy(finding = BulletinFinding.ExplicitNoWarning)
        assertEquals(WarningStatus.OfficialNoWarning(clear), status(clear))
        unknown(WarningUnknownReason.EXPIRED, status(clear, now = 500))
        unknown(WarningUnknownReason.STALE, status(clear, age = 100))
    }
    @Test fun unknownFindingCannotBecomeClear() = unknown(WarningUnknownReason.INCOMPLETE,
        status(baseline.copy(finding = BulletinFinding.Unknown)))
    @Test fun wrongDistrictOrHazardOrAuthorityRejected() {
        for (other in listOf(scope.copy(areaId = "other"), scope.copy(hazardId = "wind"), scope.copy(authority = WarningAuthority.NDMA))) {
            unknown(WarningUnknownReason.WRONG_SCOPE, status(baseline, requested = other))
        }
    }
    @Test fun untrustedOrInsecureUrlsRejected() {
        for (url in listOf("http://api.imd.gov.in/test", "https://imd.gov.in.evil.example/test", "https://fake-imd.gov.in/test", "https://attacker@imd.gov.in/test", "not a url")) {
            unknown(WarningUnknownReason.INVALID, status(baseline.copy(sourceUrl = url)))
        }
    }
    @Test fun timeBoundariesFailClosed() {
        unknown(WarningUnknownReason.FUTURE_ISSUE, status(baseline, now = 99))
        unknown(WarningUnknownReason.NOT_YET_VALID, status(baseline.copy(validFromMs = 201)))
        unknown(WarningUnknownReason.EXPIRED, status(baseline, now = 500))
        unknown(WarningUnknownReason.STALE, status(baseline, age = 100))
        unknown(WarningUnknownReason.INVALID, status(baseline.copy(validUntilMs = 100)))
        unknown(WarningUnknownReason.INVALID, status(baseline, age = 0))
        unknown(WarningUnknownReason.INVALID, status(baseline, now = -1))
        assertEquals(WarningStatus.ActiveWarning(baseline), status(baseline, now = 100))
    }
    @Test fun incompleteOfficialMessageRejected() = unknown(WarningUnknownReason.INCOMPLETE,
        status(baseline.copy(finding = BulletinFinding.Warning("", "message"))))
}

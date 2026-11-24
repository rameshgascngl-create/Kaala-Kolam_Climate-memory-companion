package edu.gascnagercoil.kaalakolam.domain.weather

import java.net.URI

/** Source identities for future, separately authorised adapters. A URL check is not authentication. */
enum class WarningAuthority(val domain: String) {
    IMD("imd.gov.in"), NDMA("ndma.gov.in"), INCOIS("incois.gov.in");

    fun acceptsSource(url: String): Boolean = runCatching {
        val uri = URI(url)
        val host = uri.host ?: return false
        uri.scheme.equals("https", ignoreCase = true) && uri.userInfo == null &&
            (host.equals(domain, ignoreCase = true) || host.endsWith(".$domain", ignoreCase = true))
    }.getOrDefault(false)
}

/** IDs are namespaced by the provider; district names are not identity keys. */
data class WarningScope(val authority: WarningAuthority, val areaId: String, val hazardId: String)

sealed interface BulletinFinding {
    data class Warning(val officialSeverity: String, val officialMessage: String) : BulletinFinding
    /** Only an explicit, scoped no-warning assertion from the source qualifies. */
    object ExplicitNoWarning : BulletinFinding
    object Unknown : BulletinFinding
}

data class WarningBulletin(
    val scope: WarningScope,
    val sourceUrl: String,
    val issuedAtMs: Long,
    val validFromMs: Long,
    val validUntilMs: Long,
    val finding: BulletinFinding,
)

enum class WarningUnknownReason {
    UNAVAILABLE, INVALID, WRONG_SCOPE, FUTURE_ISSUE, NOT_YET_VALID, EXPIRED, STALE, INCOMPLETE
}

sealed interface WarningStatus {
    data class Unknown(val reason: WarningUnknownReason) : WarningStatus
    data class ActiveWarning(val bulletin: WarningBulletin) : WarningStatus
    /** Not a guarantee of safety; never inferred from an empty response. */
    data class OfficialNoWarning(val bulletin: WarningBulletin) : WarningStatus
}

/** A pure presentation-domain policy. Provider access, authenticity, parsing and permissions are separate gates. */
object WarningStatusPolicy {
    fun evaluate(
        requested: WarningScope,
        bulletin: WarningBulletin?,
        nowMs: Long,
        maxIssueAgeMs: Long,
    ): WarningStatus {
        fun unknown(reason: WarningUnknownReason) = WarningStatus.Unknown(reason)
        if (nowMs < 0 || maxIssueAgeMs <= 0 || requested.areaId.isBlank() || requested.hazardId.isBlank()) {
            return unknown(WarningUnknownReason.INVALID)
        }
        if (bulletin == null) return unknown(WarningUnknownReason.UNAVAILABLE)
        if (bulletin.scope != requested) return unknown(WarningUnknownReason.WRONG_SCOPE)
        if (!requested.authority.acceptsSource(bulletin.sourceUrl) || bulletin.issuedAtMs < 0 ||
            bulletin.validFromMs < 0 || bulletin.validUntilMs <= bulletin.validFromMs ||
            bulletin.issuedAtMs >= bulletin.validUntilMs) {
            return unknown(WarningUnknownReason.INVALID)
        }
        if (bulletin.issuedAtMs > nowMs) return unknown(WarningUnknownReason.FUTURE_ISSUE)
        if (nowMs >= bulletin.validUntilMs) return unknown(WarningUnknownReason.EXPIRED)
        if (nowMs < bulletin.validFromMs) return unknown(WarningUnknownReason.NOT_YET_VALID)
        if (nowMs - bulletin.issuedAtMs >= maxIssueAgeMs) return unknown(WarningUnknownReason.STALE)
        return when (val finding = bulletin.finding) {
            is BulletinFinding.Warning -> if (finding.officialSeverity.isBlank() || finding.officialMessage.isBlank()) {
                unknown(WarningUnknownReason.INCOMPLETE)
            } else WarningStatus.ActiveWarning(bulletin)
            BulletinFinding.ExplicitNoWarning -> WarningStatus.OfficialNoWarning(bulletin)
            BulletinFinding.Unknown -> unknown(WarningUnknownReason.INCOMPLETE)
        }
    }
}

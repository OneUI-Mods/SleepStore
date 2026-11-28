package dev.ryuya.sleepstore

internal object DeepSleepRepairPolicy {
    val packageName = Regex("[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z0-9_]+)+")

    private val sleepEntry = Regex(
        """-DR\s+([12])-DD T-Uid\s+(\d+)\(-\)-curLv 4-maxLv 4-FAS Y/.*?-Pkg ([\w.]+)$""",
    )
    private val stores = setOf("com.android.vending", "com.sec.android.app.samsungapps")

    fun sleepingPackages(mars: String, user: Int): Map<String, Int> =
        mars.lineSequence().mapNotNull { line ->
            val match = sleepEntry.find(line.trimEnd()) ?: return@mapNotNull null
            val uid = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
            if (uid / 100000 != user || uid % 100000 < 10000) return@mapNotNull null
            val name = match.groupValues[3]
            if (!packageName.matches(name) || name in stores || name == BuildConfig.APPLICATION_ID) {
                return@mapNotNull null
            }
            name to uid
        }.toMap()

    fun wasResetByStore(enabled: String?, enabledCaller: String?, stopped: String?): Boolean =
        (enabled == null || enabled == "0") && enabledCaller in stores && stopped == "true"
}

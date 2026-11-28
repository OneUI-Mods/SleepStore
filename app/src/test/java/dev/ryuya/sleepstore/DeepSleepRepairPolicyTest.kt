package dev.ryuya.sleepstore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepSleepRepairPolicyTest {
    @Test
    fun selectsIdleDeepSleepEntriesForOneProfile() {
        val dump = """
            -DR 2-DD T-Uid 10570(-)-curLv 4-maxLv 4-FAS Y/ 2(-)-BU 0.000000-Pkg example.camera
            -DR 1-DD T-Uid 1010570(-)-curLv 4-maxLv 4-FAS Y/ 2(-)-BU 0.000000-Pkg example.camera
            -DR 2-DD T-Uid 10571(-)-curLv 4-maxLv 4-FAS Y/ 2(-)-BU 0.000000-Pkg example.active--(R)
            -DR 2-DD T-Uid 10572(-)-curLv 2-maxLv 4-FAS Y/ 2(-)-BU 0.000000-Pkg example.awake
            -DR 2-DD F-Uid 10573(-)-curLv 4-maxLv 4-FAS Y/ 2(-)-BU 0.000000-Pkg example.notDisabled
            -DR 2-DD T-Uid 10574(-)-curLv 4-maxLv 4-FAS Y/ 2(-)-BU 0.000000-Pkg com.android.vending
            -DR 2-DD T-Uid 10575(S)-curLv 4-maxLv 4-FAS Y/ 2(-)-BU 0.000000-Pkg example.shared
        """.trimIndent()
        assertEquals(mapOf("example.camera" to 10570), DeepSleepRepairPolicy.sleepingPackages(dump, 0))
        assertEquals(mapOf("example.camera" to 1010570), DeepSleepRepairPolicy.sleepingPackages(dump, 10))
    }

    @Test
    fun requiresARecognizedStoreResetAndStoppedState() {
        with(DeepSleepRepairPolicy) {
            assertTrue(wasResetByStore("0", "com.android.vending", "true"))
            assertTrue(wasResetByStore(null, "com.sec.android.app.samsungapps", "true"))
            assertFalse(wasResetByStore("4", "auto_disabler", "true"))
            assertFalse(wasResetByStore("3", "com.android.vending", "true"))
            assertFalse(wasResetByStore("0", "com.android.vending", "false"))
            assertFalse(wasResetByStore("0", "com.sec.android.app.launcher", "true"))
        }
    }
}

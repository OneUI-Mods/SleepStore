package dev.ryuya.sleepstore

import kotlin.jvm.JvmStatic

/* A Samsung auto-disable signal is required in addition to DISABLED_UNTIL_USED. */
object Eligibility {
    @JvmStatic
    fun isDeepSleeping(state: Int, samsungAutoDisabled: Boolean): Boolean {
        return state == 4 && samsungAutoDisabled
    }
}

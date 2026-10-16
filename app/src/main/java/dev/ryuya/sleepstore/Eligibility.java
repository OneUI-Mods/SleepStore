package dev.ryuya.sleepstore;

/** A Samsung auto-disable signal is required in addition to DISABLED_UNTIL_USED. */
public final class Eligibility {
    private Eligibility() {}
    public static boolean isDeepSleeping(int state, boolean samsungAutoDisabled) {
        return state == 4 && samsungAutoDisabled;
    }
}

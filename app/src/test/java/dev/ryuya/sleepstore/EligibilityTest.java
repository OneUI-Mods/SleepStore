package dev.ryuya.sleepstore;
import org.junit.Test;
import static org.junit.Assert.*;
public class EligibilityTest {
    @Test public void manualDisableAndUnknownSignalsAreNeverOverridden() {
        for(int state:new int[]{0,1,2,3,5,-1}) for(boolean signal:new boolean[]{false,true}) assertFalse(Eligibility.isDeepSleeping(state,signal));
        assertFalse(Eligibility.isDeepSleeping(4,false));
        assertTrue(Eligibility.isDeepSleeping(4,true));
    }
}

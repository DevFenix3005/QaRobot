package com.rebirth.qarobot.app.main;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainRoutingTest {
    @Test
    void preservesGuiLaunchAndLegacyTimeoutButRoutesCommandsAndMistakesToCli() {
        assertFalse(Main.isCliInvocation(new String[0]));
        assertFalse(Main.isCliInvocation(new String[]{"999"}));
        assertTrue(Main.isCliInvocation(new String[]{"run", "scenario.xml"}));
        assertTrue(Main.isCliInvocation(new String[]{"--help"}));
        assertTrue(Main.isCliInvocation(new String[]{"typo"}));
        assertTrue(Main.isCliInvocation(new String[]{"999", "unexpected"}));
    }
}

package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneAlertTest {
    @Test fun watchFirstWhenReachable() =
        assertEquals(PhoneAlert.Mode.SILENT, PhoneAlert.mode(Wake.NotifyKind.QUESTION, watchPaired = true, watchReachable = true))

    @Test fun soundsWhenWatchOffOrAway() =
        assertEquals(PhoneAlert.Mode.SOUND, PhoneAlert.mode(Wake.NotifyKind.QUESTION, watchPaired = true, watchReachable = false))

    @Test fun soundsWithoutWatch() =
        assertEquals(PhoneAlert.Mode.SOUND, PhoneAlert.mode(Wake.NotifyKind.OUTCOME, watchPaired = false, watchReachable = false))

    @Test fun quotaIsAlwaysSilent() =
        assertEquals(PhoneAlert.Mode.SILENT, PhoneAlert.mode(Wake.NotifyKind.QUOTA, watchPaired = false, watchReachable = false))
}

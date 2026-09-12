package it.pixelbox.cmwatch

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SmokeTest {
    @Test fun contractFixturesAreReachable() {
        assertTrue(File("../contract/state-1-question.json").isFile)
    }
}

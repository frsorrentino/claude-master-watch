package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPreviewTest {
    @Test fun imagesUpTo10MbArriveOnTheirOwnOnAnyNetwork() {
        assertTrue(MediaPreview.auto("image/png", 24_645, unmetered = false))
        assertTrue(MediaPreview.auto("image/jpeg", 10_000_000, unmetered = false))
        assertTrue(MediaPreview.auto("image/png", null, unmetered = false))
        assertFalse(MediaPreview.auto("image/png", 10_000_001, unmetered = true))
    }

    @Test fun videosUpTo25MbArriveOnTheirOwnOnlyWithoutAMeteredNetwork() {
        assertTrue(MediaPreview.auto("video/mp4", 7_211_071, unmetered = true))
        assertTrue(MediaPreview.auto("video/mp4", 26_214_400, unmetered = true))
        assertFalse(MediaPreview.auto("video/mp4", 7_211_071, unmetered = false))
        assertFalse(MediaPreview.auto("video/mp4", 31_291_332, unmetered = true))
        // Senza misura un video potrebbe essere enorme: aspetta il tocco.
        assertFalse(MediaPreview.auto("video/mp4", null, unmetered = true))
    }

    @Test fun otherFilesWaitForTheTap() {
        assertFalse(MediaPreview.auto("application/pdf", 1_000, unmetered = true))
        assertFalse(MediaPreview.auto(null, 1_000, unmetered = true))
        assertTrue(MediaPreview.isVideo("video/webm"))
        assertFalse(MediaPreview.isVideo("image/gif"))
    }
}

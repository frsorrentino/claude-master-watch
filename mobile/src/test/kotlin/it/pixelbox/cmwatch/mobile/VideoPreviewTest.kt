package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.VideoPoster
import it.pixelbox.cmwatch.mobile.ui.clockOf
import it.pixelbox.cmwatch.ui.tokens.CmColors
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Il riquadro fermo di un video della chat, prima del tocco (Franz, 10/10 15:24: «Ok anteprime video»). */
class VideoPreviewTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    /** Un fotogramma finto, cielo, prato e sole, per vedere ritaglio e proporzioni. */
    private fun frame(w: Int, h: Int): ImageBitmap {
        val b = ImageBitmap(w, h)
        val c = Canvas(b)
        val p = Paint()
        p.color = Color(0xFF3B6EA8); c.drawRect(0f, 0f, w.toFloat(), h * .6f, p)
        p.color = Color(0xFF4F8A3C); c.drawRect(0f, h * .6f, w.toFloat(), h.toFloat(), p)
        p.color = Color(0xFFF2C14E); c.drawCircle(Offset(w * .75f, h * .3f), minOf(w, h) * .12f, p)
        return b
    }

    @Test fun landscapePoster() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            Box(Modifier.background(CmColors.bg).padding(12.dp)) { VideoPoster(frame(320, 180), 24_000, 16f / 9f) {} }
        }
    }

    /** Il taglio per i social, 9×16: alto 320 dp invece che largo 280. */
    @Test fun portraitPoster() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            Box(Modifier.background(CmColors.bg).padding(12.dp)) { VideoPoster(frame(180, 320), 312_000, 9f / 16f) {} }
        }
    }

    @Test fun durationsReadLikeAPlayer() {
        assertEquals("0:24", clockOf(24_000))
        assertEquals("5:12", clockOf(312_000))
        assertEquals("1:02:33", clockOf(3_753_000))
    }
}

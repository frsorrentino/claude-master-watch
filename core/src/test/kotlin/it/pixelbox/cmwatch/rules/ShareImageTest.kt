package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

class ShareImageTest {
    @Test fun longSideTo1600KeepingProportions() = assertEquals(1600 to 900, ShareImage.scaled(3200, 1800))
    @Test fun portraitToo() = assertEquals(720 to 1600, ShareImage.scaled(1440, 3200))
    @Test fun smallImagesStayAsTheyAre() = assertEquals(800 to 600, ShareImage.scaled(800, 600))
    /** Il campionamento di BitmapFactory: la potenza di 2 più grande che resta sopra la misura voluta. */
    @Test fun sampleSize() {
        assertEquals(2, ShareImage.sampleSize(4000, 3000))
        assertEquals(1, ShareImage.sampleSize(1600, 1200))
        assertEquals(4, ShareImage.sampleSize(8000, 6000))
    }
}

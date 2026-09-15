package it.pixelbox.cmwatch.wear

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.tiles.renderer.TileRenderer
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenRound
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.wear.tile.CmTileService
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

/**
 * La tile disegnata dal codice, in inglese sui dati di prova, per il README (Franz, 15/09 14:35: la foto dal polso in
 * italiano stonava fra gli snapshot). Il layout lo costruisce `CmTileService.root`, lo disegna il renderer delle tile.
 */
class TileSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.WEAR_OS_SMALL_ROUND.copy(screenWidth = 456, screenHeight = 456, density = Density.XHIGH, screenRound = ScreenRound.ROUND, locale = "en"),
        theme = "android:Theme.DeviceDefault.NoActionBar",
    )

    private val state = ContractJson.decodeState(File("../contract/state-2-idle.json").readText())

    private val device = DeviceParametersBuilders.DeviceParameters.Builder()
        .setScreenWidthDp(228).setScreenHeightDp(228).setScreenDensity(2f)
        .setScreenShape(DeviceParametersBuilders.SCREEN_SHAPE_ROUND)
        .setDevicePlatform(DeviceParametersBuilders.DEVICE_PLATFORM_WEAR_OS)
        .build()

    // In CI il renderer delle tile non trova il suo tema (`androidx.wear.protolayout.renderer.R$style`, run 34996843873),
    // anche con le risorse Android incluse nei test: fermo finché non si trova il modo di dargli le sue risorse.
    @org.junit.Ignore("TileRenderer cannot load its R\$style under Paparazzi yet")
    @Test fun tile() {
        val service = object : CmTileService() { fun attach(c: Context) = attachBaseContext(c) }
        service.attach(paparazzi.context)
        val root = service.root(device, state, Freshness.Fresh, emptySet(), "personal", state.ts)
        val layout = LayoutElementBuilders.Layout.Builder().setRoot(root).build()
        val frame = FrameLayout(paparazzi.context).apply {
            layoutParams = ViewGroup.LayoutParams(456, 456)
            setBackgroundColor(Color.BLACK)
        }
        val renderer = TileRenderer(paparazzi.context, Executor { it.run() }) { }
        renderer.inflateAsync(layout, service.tileResources(), frame).get(20, TimeUnit.SECONDS)
        paparazzi.snapshot(frame)
    }
}

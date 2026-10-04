package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.mobile.ui.AddDeviceSheet
import it.pixelbox.cmwatch.mobile.ui.AddDeviceUi
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.rules.PairAddText
import it.pixelbox.cmwatch.ui.tokens.CmColors
import org.junit.Rule
import org.junit.Test

/** Contratto 1.31 (Franz, 04/10 13:47): l'invito per un dispositivo in più, mostrato sul telefono. */
class AddDeviceSheetTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    /** L'invito della fixture del relay (`cmd-result-sample`, id …0200): il QR di `pair-add.json` e il codice 482913. */
    private val invite = it.pixelbox.cmwatch.contract.PairAddInvite("{\"v\":1,\"i\":\"q3Vb2mXz0rT8yKp1LwN4sA\",\"c\":\"j0DFrbaPJWJK5bIU6nZ6bslNgp09e14a0bpvPiE4KF8=\",\"h\":\"penguin\",\"e\":1789211100,\"f\":{\"k\":\"AIzaSyD-example-key-000000000000000000\",\"p\":\"cmwatch-demo\",\"a\":\"1:123456789012:android:0a1b2c3d4e5f6a7b8c9d0e\",\"d\":\"https://cmwatch-demo-default-rtdb.europe-west1.firebasedatabase.app\",\"t\":\"watch\"},\"m\":\"add\"}", "482913", 1789211100L)

    @Test fun inviteWithQr() = paparazzi.snapshot {
        CmPhoneTheme(still = true) { Column(Modifier.background(CmColors.surface)) { AddDeviceSheet(AddDeviceUi.Offer(invite.qr, invite.code, invite.exp), {}, {}, now = invite.exp - 245) } }
    }

    @Test fun inviteExpired() = paparazzi.snapshot {
        CmPhoneTheme(still = true) { Column(Modifier.background(CmColors.surface)) { AddDeviceSheet(AddDeviceUi.Offer(invite.qr, invite.code, invite.exp), {}, {}, now = invite.exp + 1) } }
    }

    @Test fun pairingAlreadyOpen() = paparazzi.snapshot {
        CmPhoneTheme(still = true) { Column(Modifier.background(CmColors.surface)) { AddDeviceSheet(AddDeviceUi.Refused(PairAddText.Refusal.BUSY, ""), {}, {}) } }
    }
}

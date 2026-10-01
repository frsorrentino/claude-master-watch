package it.pixelbox.cmwatch.mobile.widget

import kotlin.math.roundToInt

/**
 * Le misure del widget, copiate da ads-widget (`computeLayoutMetrics` in `widget-constants.ts`, Franz 01/10 13:22:
 * «accorda meglio a ads widget»): una scala `sf` dalla taglia, poi ogni misura proporzionale con i suoi limiti. Sotto
 * una riga (73 dp) il widget è una striscia («ticker»): testata compatta, arco aperto in basso, quattro colonne uguali.
 * Testi in sp, spazi in dp.
 */
object WidgetLayout {
    data class M(
        val ticker: Boolean, val sf: Float,
        val padV: Int, val padH: Int, val radius: Int,
        val headerHeight: Int, val headerInset: Int, val headerBottom: Int,
        val arrow: Int, val title: Int, val time: Int,
        val chip: Int, val chipRadius: Int, val refreshGlyph: Int, val refreshBox: Int,
        val ring: Int, val ringStroke: Int, val ringValue: Int, val ringLabel: Int, val gauge: Boolean,
        val icon: Int, val value: Int, val label: Int, val gap: Int,
        /** L'icona accanto al numero nelle colonne: quella della striscia di ads-widget (18 sf) in ogni taglia. */
        val tileIcon: Int,
    )

    fun of(width: Float, height: Float): M {
        val rows = (height / 73f).roundToInt()
        val ticker = rows <= 1
        val sf = (if (ticker) minOf(width / 300f, height / 55f) else minOf(width / 292f, height / 219f)).coerceIn(0.5f, 1.15f)
        fun r(v: Float, lo: Float, hi: Float) = v.coerceIn(lo, hi).roundToInt()
        fun s(v: Float) = (v * sf).roundToInt()
        val refresh = if (ticker) r(17 * sf, 12f, 28f) else r(22 * sf, 12f, 28f)
        val chip = (refresh * 1.15f).roundToInt()
        return M(
            ticker = ticker, sf = sf,
            padV = if (ticker) 1 else r(14 * sf, 0f, 20f),
            padH = if (ticker) 4 else r(14 * sf, 2f, 20f),
            radius = r(24 * sf, 8f, 32f),
            // La riga della testata è alta quanto la pastiglia di ↻ con il suo margine (2 dp sopra e sotto).
            headerHeight = chip + 2 * s(2f),
            headerInset = if (ticker) s(2f) + s(3f) else s(4f) + s(4f),
            headerBottom = if (ticker) s(1f) else r(6 * sf, 2f, 10f),
            arrow = if (ticker) s(6f) else s(10f),
            title = if (ticker) r(11 * sf, 7f, 15f) else r(12 * sf, 9f, 17f),
            time = r(10 * sf, 7f, 13f),
            chip = chip, chipRadius = r(8 * sf, 3f, 12f),
            refreshGlyph = (refresh * (if (ticker) 0.75f else 0.85f)).roundToInt(),
            refreshBox = (refresh * 2.5f).roundToInt(),
            ring = if (ticker) r(64 * sf, 42f, 76f) else if (height >= 200) r(100 * sf, 70f, 130f) else r(80 * sf, 50f, 100f),
            ringStroke = r(5 * sf, 3f, 7f),
            ringValue = if (ticker) r(16 * sf, 9f, 22f) else r(16 * sf, 10f, 22f),
            ringLabel = if (ticker) r(7 * sf, 4f, 10f) else r(8 * sf, 6f, 11f),
            gauge = ticker,
            icon = if (ticker) r(18 * sf, 7f, 24f) else r(12 * sf, 7f, 24f),
            // Misurato sul telefono di Franz (01/10 13:22): le cifre dei KPI di ads-widget escono a 24sp con sf 1,15,
            // non ai 29 della sua formula; la proporzione è 21 sf.
            value = if (ticker) r(21 * sf, 12f, 28f) else r(22 * sf, 12f, 32f),
            label = if (ticker) r(10 * sf, 6f, 13f) else r(10 * sf, 7f, 14f),
            gap = s(3f),
            tileIcon = r(18 * sf, 7f, 24f),
        )
    }
}

package it.pixelbox.cmwatch.mobile.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import it.pixelbox.cmwatch.rules.ShareImage
import java.io.ByteArrayOutputStream

/** L'immagine condivisa ridotta a 1600 px sul lato lungo, JPEG q85 (contratto 1.19); null se non si legge. */
object ImageShrink {
    fun jpeg(ctx: Context, uri: Uri): ByteArray? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null
        val opts = BitmapFactory.Options().apply { inSampleSize = ShareImage.sampleSize(bounds.outWidth, bounds.outHeight) }
        val raw = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val (w, h) = ShareImage.scaled(raw.width, raw.height)
        val bmp = if (w == raw.width && h == raw.height) raw else Bitmap.createScaledBitmap(raw, w, h, true)
        ByteArrayOutputStream().use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 85, out); out.toByteArray() }
    }.getOrNull()
}

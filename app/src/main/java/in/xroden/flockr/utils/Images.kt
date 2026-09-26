/** Shrinks a picture the user chose into the JPEG Flockr uploads. */
package `in`.xroden.flockr.utils

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

private const val LONGEST_SIDE = 1024
private const val JPEG_QUALITY = 80

/**
 * [image] decoded upright (EXIF rotation applied), scaled so its longest side is at most 1024 pixels,
 * and re-encoded as JPEG. Null when the bytes aren't a picture Android can read. Blocks, so call it off
 * the main thread.
 */
fun uploadJpegOf(image: ByteArray): ByteArray? = runCatching {
    val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(image))) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val scale = LONGEST_SIDE.toFloat() / maxOf(info.size.width, info.size.height)
        if (scale < 1f) decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
    }
    ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }.toByteArray()
}.getOrNull()

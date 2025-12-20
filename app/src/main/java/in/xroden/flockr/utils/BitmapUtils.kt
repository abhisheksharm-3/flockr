package `in`.xroden.flockr.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import kotlin.math.min
import javax.inject.Inject

class BitmapUtils @Inject constructor() {

    fun compressImage(imageData: ByteArray, maxSize: Int = 1024): ByteArray {
        // First decode with inJustDecodeBounds to check dimensions
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(imageData, 0, imageData.size, options)

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        val scaledBitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size, options)
            ?: return imageData // Return original if decoding fails (shouldn't happen)

        // Compress
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        
        return outputStream.toByteArray()
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}

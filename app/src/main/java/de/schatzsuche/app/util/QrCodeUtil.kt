package de.schatzsuche.app.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import de.schatzsuche.app.data.model.QrCodeEntity
import java.util.UUID

object QrCodeUtil {
    const val PAYLOAD_PREFIX = "schatzsuche://qr/"

    fun createPayload(codeId: String): String = "$PAYLOAD_PREFIX$codeId"

    fun parsePayload(raw: String): String? {
        return if (raw.startsWith(PAYLOAD_PREFIX)) {
            raw.removePrefix(PAYLOAD_PREFIX)
        } else {
            null
        }
    }

    fun generateCodes(count: Int): List<QrCodeEntity> {
        return (1..count).map { number ->
            val codeId = UUID.randomUUID().toString()
            QrCodeEntity(
                codeId = codeId,
                number = number,
                payload = createPayload(codeId)
            )
        }
    }

    fun generateBitmap(payload: String, size: Int = 512): Bitmap {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}

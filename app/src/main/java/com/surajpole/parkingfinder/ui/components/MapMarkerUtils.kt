package com.surajpole.parkingfinder.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.surajpole.parkingfinder.ui.theme.EmeraldGreen
import com.surajpole.parkingfinder.ui.theme.NavyPrimary

object MapMarkerUtils {
    private var cachedUserDot: BitmapDescriptor? = null

    /**
     * Generates a sleek, unique host marker icon (Electric Cyan radar beacon)
     * distinctly different from Google Maps default blue dot.
     */
    fun getUserLocationDescriptor(): BitmapDescriptor {
        cachedUserDot?.let { return it }

        val size = 64
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val center = size / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Outer glow aura
        paint.color = android.graphics.Color.parseColor("#4D00E5FF") // 30% translucent cyan
        canvas.drawCircle(center, center, center * 0.95f, paint)

        // Inner translucent halo
        paint.color = android.graphics.Color.parseColor("#8000B0FF")
        canvas.drawCircle(center, center, center * 0.70f, paint)

        // Crisp white ring border
        paint.color = android.graphics.Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        canvas.drawCircle(center, center, center * 0.45f, paint)

        // Vibrant Core host dot
        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.parseColor("#00E5FF") // Electric Cyan
        canvas.drawCircle(center, center, center * 0.40f, paint)

        // Center tiny white highlight pip
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(center, center, center * 0.15f, paint)

        val descriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
        cachedUserDot = descriptor
        return descriptor
    }
}

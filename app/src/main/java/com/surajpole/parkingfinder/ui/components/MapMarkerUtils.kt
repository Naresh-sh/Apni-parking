package com.surajpole.parkingfinder.ui.components

import android.graphics.*
import androidx.collection.LruCache
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.surajpole.parkingfinder.data.model.ParkingLot

object MapMarkerUtils {
    private var cachedDirectionalDot: BitmapDescriptor? = null
    private val markerCache = LruCache<String, BitmapDescriptor>(80)

    /**
     * Creates Google Maps style directional user locator with a glowing forward beam.
     * The forward beam points directly north (0 deg), so setting Marker.rotation = azimuth
     * rotates the beam smoothly via hardware acceleration on the GPU at 120 FPS.
     */
    fun getUserDirectionalDescriptor(): BitmapDescriptor {
        cachedDirectionalDot?.let { return it }

        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = size / 2f

        val beamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = RadialGradient(
                center,
                center,
                center * 0.95f,
                intArrayOf(
                    Color.argb(160, 37, 99, 235), // Vibrant royal blue
                    Color.argb(80, 59, 130, 246),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0.1f, 0.6f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }

        // Draw 60-degree forward radar cone / beam pointing upwards (0 deg)
        val path = Path().apply {
            moveTo(center, center)
            arcTo(
                center - center * 0.95f,
                center - center * 0.95f,
                center + center * 0.95f,
                center + center * 0.95f,
                240f, // From 240 deg to 300 deg (60-deg cone pointing North)
                60f,
                false
            )
            close()
        }
        canvas.drawPath(path, beamPaint)

        // Outer white ring
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
            color = Color.WHITE
            setShadowLayer(6f, 0f, 2f, Color.argb(120, 0, 0, 0))
        }
        canvas.drawCircle(center, center, 18f, ringPaint)

        // Core Royal Blue user dot
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#2563EB")
        }
        canvas.drawCircle(center, center, 17f, dotPaint)

        // Center white pip
        val pipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        canvas.drawCircle(center, center, 6f, pipPaint)

        val descriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
        cachedDirectionalDot = descriptor
        return descriptor
    }

    /**
     * Cute, attractive modern parking pins.
     * When isZoomed = false: Compact cute pin with badge.
     * When isZoomed = true: Cute pin with a stylish floating pill showing parking name & free slots.
     */
    fun getCuteParkingDescriptor(lot: ParkingLot, isZoomedIn: Boolean): BitmapDescriptor {
        val freeSlots = (lot.total_slots - lot.filled_slots).coerceAtLeast(0)
        val cacheKey = "${lot.id}_${isZoomedIn}_$freeSlots"
        markerCache.get(cacheKey)?.let { return it }

        val statusColor = when {
            lot.isFull -> Color.parseColor("#EF4444") // Red
            freeSlots <= 5 -> Color.parseColor("#F59E0B") // Amber
            else -> Color.parseColor("#10B981") // Emerald Green
        }

        val bitmap = if (!isZoomedIn) {
            drawCompactCutePin(statusColor)
        } else {
            drawZoomedCutePinWithName(lot.name, freeSlots, statusColor)
        }

        val descriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
        markerCache.put(cacheKey, descriptor)
        return descriptor
    }

    private fun drawCompactCutePin(statusColor: Int): Bitmap {
        val width = 76
        val height = 90
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Shadow under tip
        paint.color = Color.argb(60, 0, 0, 0)
        canvas.drawOval(width / 2f - 12f, height - 8f, width / 2f + 12f, height.toFloat(), paint)

        // Pin Body Path
        val pinPath = Path().apply {
            val cx = width / 2f
            val cy = 34f
            val r = 30f
            arcTo(cx - r, cy - r, cx + r, cy + r, 150f, 240f, false)
            lineTo(cx, height - 8f)
            close()
        }

        // Gradient pin fill (Royal Navy to Deep Slate)
        paint.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            Color.parseColor("#1E3A8A"),
            Color.parseColor("#0F172A"),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(pinPath, paint)
        paint.shader = null

        // Pin White outline
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = Color.WHITE
        canvas.drawPath(pinPath, paint)

        // Inner Circle
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawCircle(width / 2f, 34f, 19f, paint)

        // Bold 'P' inside
        paint.color = Color.parseColor("#1E3A8A")
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("P", width / 2f, 42f, paint)

        // Cute status badge dot (top-right of pin)
        paint.color = Color.WHITE
        canvas.drawCircle(width / 2f + 18f, 16f, 9f, paint)
        paint.color = statusColor
        canvas.drawCircle(width / 2f + 18f, 16f, 7f, paint)

        return bitmap
    }

    private fun drawZoomedCutePinWithName(name: String, freeSlots: Long, statusColor: Int): Bitmap {
        val shortName = if (name.length > 20) name.take(18) + "…" else name
        val labelText = "$shortName  •  $freeSlots free"

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.parseColor("#0F172A")
        }

        val textBounds = Rect()
        textPaint.getTextBounds(labelText, 0, labelText.length, textBounds)

        val pillPadding = 24f
        val pillWidth = (textBounds.width() + pillPadding * 2).coerceAtLeast(140f)
        val pillHeight = 48f

        val pinWidth = 60f
        val pinHeight = 72f

        val totalWidth = pillWidth.coerceAtLeast(pinWidth).toInt() + 20
        val totalHeight = (pillHeight + 10f + pinHeight).toInt()

        val bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = totalWidth / 2f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Draw Cute Floating Pill
        val pillRect = RectF(
            cx - pillWidth / 2f,
            4f,
            cx + pillWidth / 2f,
            4f + pillHeight
        )

        // Pill shadow
        paint.color = Color.argb(45, 0, 0, 0)
        canvas.drawRoundRect(
            RectF(pillRect.left, pillRect.top + 2f, pillRect.right, pillRect.bottom + 4f),
            24f, 24f, paint
        )

        // Pill background
        paint.color = Color.WHITE
        canvas.drawRoundRect(pillRect, 24f, 24f, paint)

        // Pill border with status color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = statusColor
        canvas.drawRoundRect(pillRect, 24f, 24f, paint)

        // Status indicator dot inside pill
        paint.style = Paint.Style.FILL
        paint.color = statusColor
        canvas.drawCircle(pillRect.left + 16f, pillRect.centerY(), 6f, paint)

        // Text inside pill
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(
            labelText,
            pillRect.left + 28f,
            pillRect.centerY() + textBounds.height() / 2f - 2f,
            textPaint
        )

        // 2. Draw Pin underneath
        val pinTop = 4f + pillHeight + 6f
        val pinBottom = pinTop + pinHeight - 8f

        val pinPath = Path().apply {
            val cy = pinTop + 24f
            val r = 22f
            arcTo(cx - r, cy - r, cx + r, cy + r, 150f, 240f, false)
            lineTo(cx, pinBottom)
            close()
        }

        // Shadow under pin tip
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(60, 0, 0, 0)
        canvas.drawOval(cx - 10f, pinBottom - 2f, cx + 10f, pinBottom + 6f, paint)

        // Pin gradient
        paint.shader = LinearGradient(
            0f, pinTop, 0f, pinBottom,
            Color.parseColor("#1E3A8A"),
            Color.parseColor("#0F172A"),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(pinPath, paint)
        paint.shader = null

        // Pin border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = Color.WHITE
        canvas.drawPath(pinPath, paint)

        // Inner white circle with 'P'
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawCircle(cx, pinTop + 24f, 14f, paint)

        paint.color = Color.parseColor("#1E3A8A")
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("P", cx, pinTop + 30f, paint)

        return bitmap
    }
}

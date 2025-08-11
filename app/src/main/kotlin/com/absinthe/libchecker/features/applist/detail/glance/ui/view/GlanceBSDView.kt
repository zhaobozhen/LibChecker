package com.absinthe.libchecker.features.applist.detail.glance.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withRotation
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.displayWidth
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.manager.SystemBarManager
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class GlanceBSDView(context: Context) :
  LinearLayout(context),
  IHeaderView {

  private val header = BottomSheetHeaderView(context).apply {
    layoutParams =
      LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    title.text = context.getString(R.string.app_info_glance)
  }

  val image = AppCompatImageView(context).apply {
    // TODO Landscape support
    val width = context.displayWidth * 2 / 3
    layoutParams = LayoutParams(width, width * 4 / 3).also {
      it.topMargin = 16.dp
    }
  }

  init {
    orientation = VERTICAL
    gravity = Gravity.CENTER_HORIZONTAL
    val padding = 16.dp
    setPadding(
      padding,
      padding,
      padding,
      (padding - SystemBarManager.navigationBarSize).coerceAtLeast(0)
    )
    addView(header)
    addView(image)
  }

  override fun getHeaderView(): BottomSheetHeaderView {
    return header
  }

  fun drawImage(info: Info, backgroundColor: Int) {
    if (image.width <= 0 || image.height <= 0) {
      return
    }
    val bitmap = createBitmap(image.width, image.height)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
      isAntiAlias = true
      color = backgroundColor
    }
    val cornerRadius = 16.dp.toFloat()
    canvas.drawRoundRect(
      0f,
      0f,
      image.width.toFloat(),
      image.height.toFloat(),
      cornerRadius,
      cornerRadius,
      paint
    )

    drawTitleContent(canvas, info)
    drawPlanet(canvas, info, backgroundColor)
    drawCapsules(canvas, info)
    drawSatellites(canvas, info)

    drawLogo(canvas)
    if (BuildConfig.IS_DEV_VERSION) {
      drawVersionCode(canvas)
    }

    image.setImageBitmap(bitmap)
  }

  private fun drawTitleContent(canvas: Canvas, info: Info) {
    val textPaint = Paint().apply {
      isAntiAlias = true
      color = Color.WHITE
      alpha = (255 * 0.618).toInt()
      textSize = 14.dp.toFloat()
      typeface = Typeface.DEFAULT_BOLD
      textAlign = Paint.Align.CENTER
    }

    val textX = image.width / 2f

    // Draw app name
    val appNameFontMetrics = textPaint.fontMetrics
    val appNameY = padding - appNameFontMetrics.ascent
    canvas.drawText(info.appName, textX, appNameY, textPaint)

    // Draw version name
    textPaint.typeface = Typeface.DEFAULT
    textPaint.alpha = (255 * 0.382).toInt()
    textPaint.textSize = 10.dp.toFloat()
    val versionNameFontMetrics = textPaint.fontMetrics
    val versionNameY = appNameY + appNameFontMetrics.descent + 4.dp - versionNameFontMetrics.ascent
    val versionText = "${info.versionName} (${info.versionCode})"
    canvas.drawText(versionText, textX, versionNameY, textPaint)
  }

  private fun drawPlanet(canvas: Canvas, info: Info, backgroundColor: Int) {
    val centerX = image.width / 2f
    val centerY = image.height * 0.45f
    val iconSize = image.width * 0.382f

    // Draw satellite ring
    val ringPaint = Paint().apply {
      isAntiAlias = true
      color = Color.WHITE
      alpha = (255 * 0.2).toInt()
      style = Paint.Style.STROKE
      strokeWidth = 2.dp.toFloat()
    }

    val ringWidth = iconSize * 2.2f
    val ringHeight = iconSize * 0.5f
    val rect = RectF(
      centerX - ringWidth / 2,
      centerY - ringHeight / 2,
      centerX + ringWidth / 2,
      centerY + ringHeight / 2
    )

    // Draw back of the ring
    canvas.withRotation(-30f, centerX, centerY) {
      drawArc(rect, 180f, 180f, false, ringPaint)
    }

    // Create a temporary bitmap to draw the icon and erase part of it.
    val iconBitmap = createBitmap(image.width, image.height)
    val iconCanvas = Canvas(iconBitmap)

    // Draw app icon to the temporary canvas.
    val iconX = (image.width - iconSize) / 2f
    val iconY = centerY - iconSize / 2f
    val icon = runCatching {
      PackageUtils.getPackageInfo(info.packageName)
        .applicationInfo!!.loadIcon(SystemServices.packageManager)
    }.getOrNull()
    icon?.let {
      it.setBounds(
        iconX.toInt(),
        iconY.toInt(),
        (iconX + iconSize).toInt(),
        (iconY + iconSize).toInt()
      )
      it.draw(iconCanvas)
    }

    // Erase the intersecting part from the temporary canvas.
    val erasePaint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.STROKE
      strokeWidth = ringPaint.strokeWidth + 4.dp // 2dp gap on each side
      xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    val frontRingPath = Path().apply {
      val intersectionY = rect.bottom
      moveTo(rect.left, rect.centerY())
      quadTo(rect.left, intersectionY, centerX, intersectionY)
      quadTo(rect.right, intersectionY, rect.right, rect.centerY())
    }

    iconCanvas.withRotation(-30f, centerX, centerY) {
      drawPath(frontRingPath, erasePaint)
    }

    // Draw the result to the main canvas.
    canvas.drawBitmap(iconBitmap, 0f, 0f, null)

    // Draw the front of the ring on the main canvas.
    canvas.withRotation(-30f, centerX, centerY) {
      drawPath(frontRingPath, ringPaint)
    }
  }

  private fun drawCapsules(canvas: Canvas, info: Info) {
    if (info.capsules.isEmpty()) {
      return
    }

    // --- Define Geometry & Constants ---
    val iconSize = image.width * 0.382f
    val ringRectWidth = iconSize * 2.2f
    val ringRectHeight = iconSize * 0.5f
    val capsulePadding = 8.dp
    val topPadding = 24.dp

    // --- Calculate Capsule and Ring Dimensions ---
    val angle = 30.0
    val angleRad = Math.toRadians(angle)
    val a = ringRectWidth / 2.0
    val b = ringRectHeight / 2.0
    val cosAngle = cos(angleRad)
    val sinAngle = sin(angleRad)

    // Bounding box of the rotated ring
    val boundingBoxWidth = 2 * sqrt(a * a * cosAngle * cosAngle + b * b * sinAngle * sinAngle)
    val boundingBoxHeight = 2 * sqrt(a * a * sinAngle * sinAngle + b * b * cosAngle * cosAngle)

    val ringLeftX = (image.width / 2f) - (boundingBoxWidth / 2f)
    val ringBottomY = (image.height * 0.45f) + (boundingBoxHeight / 2f)

    val capsuleWidth = ((boundingBoxWidth - 2 * capsulePadding) / 3f).toFloat()
    val capsuleHeight = capsuleWidth * 0.45f

    // --- Draw Capsules in a Grid ---
    var currentX = ringLeftX
    var currentY = ringBottomY + topPadding

    info.capsules.forEachIndexed { index, capsuleInfo ->
      if (index > 0 && index % 3 == 0) {
        // New row
        currentX = ringLeftX
        currentY += capsuleHeight + capsulePadding
      }

      drawCapsule(
        canvas,
        capsuleInfo,
        currentX.toFloat(),
        currentY.toFloat(),
        capsuleWidth,
        capsuleHeight
      )

      currentX += capsuleWidth + capsulePadding
    }
  }

  private fun drawCapsule(
    canvas: Canvas,
    info: CapsuleInfo,
    left: Float,
    top: Float,
    width: Float,
    height: Float
  ) {
    val capsuleRadius = height / 2f
    val capsuleRect = RectF(left, top, left + width, top + height)

    // 2. Draw the Capsule Body
    val capsulePaint = Paint().apply {
      isAntiAlias = true
      color = Color.WHITE
      alpha = (255 * 0.1).toInt() // Semi-transparent
    }
    canvas.drawRoundRect(capsuleRect, capsuleRadius, capsuleRadius, capsulePaint)

    // 3. Draw the Icon
    val iconDrawableSize = height - 8.dp
    val iconMargin = (height - iconDrawableSize) / 2
    val iconLeft = left + iconMargin
    val iconTop = top + iconMargin
    val iconRight = iconLeft + iconDrawableSize
    val iconBottom = iconTop + iconDrawableSize

    ContextCompat.getDrawable(context, info.iconRes)?.let {
      it.setBounds(iconLeft.toInt(), iconTop.toInt(), iconRight.toInt(), iconBottom.toInt())
      it.alpha = (255 * 0.8).toInt()
      it.draw(canvas)
    }

    // 4. Draw the Content Text
    if (info.subcontent.isNullOrEmpty()) {
      // Center single line of text
      val textPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        alpha = (255 * 0.8).toInt()
        textSize = 8.dp.toFloat()
        textAlign = Paint.Align.CENTER
      }
      val remainingSpaceCenterX = iconRight + (capsuleRect.right - iconRight) / 2f
      val textCenterY = top + height / 2
      val fontMetrics = textPaint.fontMetrics
      val textY = textCenterY - (fontMetrics.ascent + fontMetrics.descent) / 2f
      canvas.drawText(info.content, remainingSpaceCenterX, textY, textPaint)
    } else {
      // Draw two lines of text, left-aligned
      val contentPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        alpha = (255 * 0.8).toInt()
        textSize = 8.dp.toFloat()
        textAlign = Paint.Align.LEFT
      }
      val subContentPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        alpha = (255 * 0.6).toInt() // slightly more transparent
        textSize = 6.dp.toFloat() // smaller
        textAlign = Paint.Align.LEFT
      }

      val textLeft = iconRight + 2.dp
      val textPadding = 0.dp

      // Calculate total height of the text block
      val contentMetrics = contentPaint.fontMetrics
      val subContentMetrics = subContentPaint.fontMetrics
      val contentHeight = contentMetrics.descent - contentMetrics.ascent
      val subContentHeight = subContentMetrics.descent - subContentMetrics.ascent
      val totalTextHeight = contentHeight + subContentHeight + textPadding

      // Calculate Y positions
      val blockTopY = (top + height / 2) - totalTextHeight / 2
      val contentY = blockTopY - contentMetrics.ascent
      val subContentY = contentY + contentMetrics.descent - subContentMetrics.ascent + textPadding

      canvas.drawText(info.content, textLeft, contentY, contentPaint)
      canvas.drawText(info.subcontent, textLeft, subContentY, subContentPaint)
    }
  }

  private fun drawSatellites(canvas: Canvas, info: Info) {
    if (info.stars.isEmpty()) {
      return
    }

    val centerX = image.width / 2f
    val centerY = image.height * 0.45f
    val iconSize = image.width * 0.382f
    val ringWidth = iconSize * 2.2f
    val ringHeight = iconSize * 0.5f
    val a = ringWidth / 2.0
    val b = ringHeight / 2.0
    val satelliteRadius = iconSize / 10f
    val minDistance = satelliteRadius * 3f // 2 * diameter

    // 1. Select points that meet the distance constraint
    val selectedPoints = mutableListOf<Pair<Double, Double>>()
    val maxSatellites = minOf(info.stars.size, 8)
    val maxAttemptsPerSatellite = 100 // Safety break to prevent infinite loops

    for (i in 0 until maxSatellites) {
      var attempt = 0
      while (attempt < maxAttemptsPerSatellite) {
        // Generate a random angle avoiding the occluded part in front of the planet
        // With -30 degree rotation, the occluded part is roughly from 240° to 300° (4π/3 to 5π/3)
        // So we generate angles from [0, 4π/3) ∪ (5π/3, 2π)
        val angle = if (Math.random() < 0.8) {
          // Main arc: [0, 4π/3) - larger probability for more distribution
          Math.random() * (4 * Math.PI / 3)
        } else {
          // Small arc: (5π/3, 2π)
          Math.random() * (Math.PI / 3) + (5 * Math.PI / 3)
        }

        val x = a * cos(angle)
        val y = b * sin(angle)

        var tooClose = false
        for (p in selectedPoints) {
          val dist = sqrt((x - p.first).pow(2) + (y - p.second).pow(2))
          if (dist < minDistance) {
            tooClose = true
            break
          }
        }

        if (!tooClose) {
          selectedPoints.add(Pair(x, y))
          break // Found a valid point, break the inner while loop
        }
        attempt++
      }
    }

    // 2. Rotate and draw the selected satellites
    val rotationRad = Math.toRadians(-30.0)
    val cosRotation = cos(rotationRad)
    val sinRotation = sin(rotationRad)

    selectedPoints.forEachIndexed { index, point ->
      val starInfo = info.stars.getOrNull(index) ?: return@forEachIndexed
      val rotatedX = point.first * cosRotation - point.second * sinRotation
      val rotatedY = point.first * sinRotation + point.second * cosRotation
      val finalX = (rotatedX + centerX).toFloat()
      val finalY = (rotatedY + centerY).toFloat()
      drawSatellite(canvas, starInfo, finalX, finalY, satelliteRadius)
    }
  }

  private fun drawSatellite(canvas: Canvas, star: StarInfo, cx: Float, cy: Float, radius: Float) {
    // Draw the white circular background first
    val backgroundPaint = Paint().apply {
      isAntiAlias = true
      color = "#232323".toColorInt()
      // alpha = (255 * 0.618).toInt()
    }
    canvas.drawCircle(cx, cy, radius, backgroundPaint)

    // Then draw the icon on top of the background
    ContextCompat.getDrawable(context, star.iconRes)?.let {
      val iconRadius = radius * 0.6f // Make icon slightly smaller than background
      it.setBounds(
        (cx - iconRadius).toInt(),
        (cy - iconRadius).toInt(),
        (cx + iconRadius).toInt(),
        (cy + iconRadius).toInt()
      )
      it.alpha = (255 * 0.8).toInt()
      it.draw(canvas)
    }
  }

  private val logoSize = 12.dp
  private val padding = 12.dp

  private fun drawLogo(canvas: Canvas) {
    ContextCompat.getDrawable(context, R.drawable.ic_logo)?.let {
      it.mutate()
      it.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
      it.alpha = (255 * 0.382).toInt()
      it.setBounds(padding, image.height - logoSize - padding, padding + logoSize, image.height - padding)
      it.draw(canvas)
    }
  }

  private fun drawVersionCode(canvas: Canvas) {
    val textPaint = Paint().apply {
      isAntiAlias = true
      color = Color.WHITE
      alpha = (255 * 0.05).toInt()
      textSize = 10.dp.toFloat()
    }
    val text = "#${BuildConfig.VERSION_CODE}"
    val textPadding = 2.dp
    val textX = (padding + logoSize + textPadding).toFloat()
    val logoCenterY = (image.height - logoSize - padding) + logoSize / 2
    val fontMetrics = textPaint.fontMetrics
    val textY = logoCenterY - (fontMetrics.ascent + fontMetrics.descent) / 2
    canvas.drawText(text, textX, textY, textPaint)
  }

  data class Info(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val capsules: List<CapsuleInfo> = emptyList(),
    val stars: List<StarInfo> = emptyList()
  )

  data class CapsuleInfo(
    @DrawableRes val iconRes: Int,
    val content: String,
    val subcontent: String? = null
  )

  data class StarInfo(
    @DrawableRes val iconRes: Int
  )
}

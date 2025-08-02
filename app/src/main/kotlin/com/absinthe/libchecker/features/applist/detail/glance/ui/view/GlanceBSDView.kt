package com.absinthe.libchecker.features.applist.detail.glance.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.Gravity
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.displayWidth
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.IHeaderView
import com.absinthe.libraries.utils.manager.SystemBarManager
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

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

  fun drawImage(backgroundColor: Int) {
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

    drawLogo(canvas)

    if (BuildConfig.IS_DEV_VERSION) {
      drawVersionCode(canvas)
    }

    image.setImageBitmap(bitmap)
  }

  private fun drawLogo(canvas: Canvas) {
    val logoSize = 16.dp
    val padding = 16.dp
    ContextCompat.getDrawable(context, R.drawable.ic_logo)?.let {
      it.mutate()
      it.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
      it.alpha = (255 * 0.382).toInt()
      it.setBounds(padding, padding, padding + logoSize, padding + logoSize)
      it.draw(canvas)
    }
  }

  private fun drawVersionCode(canvas: Canvas) {
    val logoSize = 16.dp
    val padding = 16.dp
    val textPaint = Paint().apply {
      isAntiAlias = true
      color = Color.WHITE
      alpha = (255 * 0.382).toInt()
      textSize = 12.dp.toFloat()
    }
    val text = "#${BuildConfig.VERSION_CODE}"
    val textPadding = 2.dp
    val textX = (padding + logoSize + textPadding).toFloat()
    val logoCenterY = (image.height - logoSize - padding) + logoSize / 2
    val fontMetrics = textPaint.fontMetrics
    val textY = logoCenterY - (fontMetrics.ascent + fontMetrics.descent) / 2
    canvas.drawText(text, textX, textY, textPaint)
  }
}

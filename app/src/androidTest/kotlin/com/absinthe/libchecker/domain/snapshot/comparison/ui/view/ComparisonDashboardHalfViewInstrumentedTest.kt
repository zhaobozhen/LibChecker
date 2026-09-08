package com.absinthe.libchecker.domain.snapshot.comparison.ui.view

import android.content.res.Configuration
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.comparison.model.ComparisonDashboardSideState
import com.absinthe.libchecker.utils.extensions.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComparisonDashboardHalfViewInstrumentedTest {

  @Test
  fun keepsRowsWithinBoundsAndAccessibleWithLargeTextAndRtl() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
      for (fontScale in listOf(1f, 2f)) {
        val configuration = Configuration(instrumentation.targetContext.resources.configuration).apply {
          this.fontScale = fontScale
        }
        val context = ContextThemeWrapper(
          instrumentation.targetContext.createConfigurationContext(configuration),
          R.style.AppTheme
        )
        for (direction in listOf(View.LAYOUT_DIRECTION_LTR, View.LAYOUT_DIRECTION_RTL)) {
          for (alignment in listOf(Gravity.START, Gravity.END)) {
            val view = ComparisonDashboardHalfView(context, alignment).apply {
              layoutDirection = direction
              setPaddingRelative(7, 11, 13, 17)
            }
            val state = ComparisonDashboardSideState("2026-09-08 12:34:56 ".repeat(10), "12345", "Snapshot description")
            view.bind(state)
            view.measure(
              View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
              View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            var bottom = view.paddingTop
            for (index in 0 until view.childCount) {
              val row = view.getChildAt(index) as TextView
              assertEquals(bottom + if (index == 2) 5.dp else 0, row.top)
              assertEquals(view.paddingLeft, row.left)
              assertEquals(view.width - view.paddingRight, row.right)
              assertEquals(alignment, row.gravity and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK)
              bottom = row.bottom
            }
            assertEquals(bottom + view.paddingBottom, view.height)
            val timestamp = view.getChildAt(1) as TextView
            assertEquals(TextUtils.TruncateAt.MIDDLE, timestamp.ellipsize)
            assertTrue(timestamp.layout.getEllipsisCount(0) > 0)
            assertEquals(state.contentDescription, view.contentDescription)
            var clicked = false
            view.setOnClickListener { clicked = true }
            view.performClick()
            assertTrue(clicked)
          }
        }
      }
    }
  }
}

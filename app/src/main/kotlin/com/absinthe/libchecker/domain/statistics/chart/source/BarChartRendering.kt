package com.absinthe.libchecker.domain.statistics.chart.source

import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import info.appdev.charting.charts.BarChart
import info.appdev.charting.data.BarData
import info.appdev.charting.data.BarDataSet
import info.appdev.charting.data.BarEntryFloat
import info.appdev.charting.formatter.IAxisValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun BarChart.applySizeBarData(
  barSizes: List<Int>,
  axisFormatter: IAxisValueFormatter
) {
  val entries = barSizes.mapIndexed { index, size ->
    BarEntryFloat(index.toFloat(), size.toFloat())
  }
  val dataSet = BarDataSet(entries.toMutableList(), "").apply {
    isDrawIcons = false
    valueFormatter = IntegerFormatter()
  }
  val colors = ArrayList<Int>(barSizes.size + 1)
  repeat(barSizes.size + 1) {
    colors.add(UiUtils.getRandomColor())
  }
  dataSet.setColors(colors)
  val barData = BarData(dataSet).apply {
    setValueTextSize(10f)
    setValueTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
  }
  withContext(Dispatchers.Main) {
    xAxis.apply {
      valueFormatter = axisFormatter
      setLabelCount(barSizes.size, false)
    }
    data = barData
    highlightValues(null)
    invalidate()
  }
}

package ru.gustos.trading.visual

import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.axis.DateAxis
import org.jfree.chart.axis.NumberAxis
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.xy.StandardXYItemRenderer
import org.jfree.chart.ui.ApplicationFrame
import org.jfree.data.time.Minute
import org.jfree.data.time.TimeSeriesCollection
import org.jfree.ui.RefineryUtilities
import java.awt.BorderLayout
import java.awt.Color
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

class SimpleProfitGraph {

    var frame : ApplicationFrame= ApplicationFrame("Ta4j example - Indicators to chart")
    var panel : ChartPanel? = null

    fun drawHistory(price: ArrayList<Pair<Long, Double>>, history: ArrayList<ArrayList<Pair<Long, Double>>>): ApplicationFrame {

        changeHistory(price,history)

        // Application frame
        frame.pack()
        RefineryUtilities.centerFrameOnScreen(frame)
        frame.isVisible = true
        return frame;
    }

    fun changeHistory(price: ArrayList<Pair<Long, Double>>, history: ArrayList<ArrayList<Pair<Long, Double>>>){
        val priceDataset = TimeSeriesCollection()
        val priceSeries = org.jfree.data.time.TimeSeries("price")
        for ((time, money) in price)
            priceSeries.addOrUpdate(Minute(Date.from(Instant.ofEpochSecond(time))), money)

        priceDataset.addSeries(priceSeries)
        val cashDataset = TimeSeriesCollection()
        var n  = 1
        for (h in history){
            val cashSeries = org.jfree.data.time.TimeSeries(""+(n++))
            for ((time, money) in h)
                cashSeries.addOrUpdate(Minute(Date.from(Instant.ofEpochSecond(time))), money)
            cashDataset.addSeries(cashSeries)

        }


        val chart = ChartFactory.createTimeSeriesChart(
                "profit", // title
                "Date", // x-axis label
                "Cash", // y-axis label
                cashDataset, // data
                true, // create legend?
                true, // generate tooltips?
                false // generate URLs?
        )
        val plot = chart.plot as XYPlot
        val axis = plot.domainAxis as DateAxis
        axis.dateFormatOverride = SimpleDateFormat("yyyy-MM-dd")


        val cashAxis = NumberAxis("Price")
        cashAxis.autoRangeIncludesZero = false
        plot.setRangeAxis(1, cashAxis)
        plot.setDataset(1, priceDataset)
        plot.mapDatasetToRangeAxis(1, 1)
        val cashFlowRenderer = StandardXYItemRenderer()
        cashFlowRenderer.setSeriesPaint(0, Color.black)
        plot.setRenderer(1, cashFlowRenderer)


        if (panel!=null)
            frame.contentPane.remove(panel)
        panel = ChartPanel(chart)
        panel!!.fillZoomRectangle = true
        panel!!.isMouseWheelEnabled = true
        panel!!.preferredSize = java.awt.Dimension(1400, 800)
        frame.contentPane.add(panel,BorderLayout.CENTER)
        frame.contentPane.repaint();

    }

}
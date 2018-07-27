package ru.gustos.trading.visual

import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.ui.ApplicationFrame
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import org.jfree.ui.RefineryUtilities
import ru.efreet.trading.exchange.Instrument
import java.awt.GridLayout

class SimpleCharts(name: String, columns : Int = 5) {

    var frame : ApplicationFrame = ApplicationFrame(name)

    init {
        frame.contentPane.layout = GridLayout(5,columns)
    }

    fun addChart(name : String, data : DoubleArray) {

        doAddChart(name,data)
        // Application frame
        frame.pack()
        RefineryUtilities.centerFrameOnScreen(frame)
        frame.isVisible = true
    }

    fun doAddChart(name : String, data : DoubleArray){
        var xy = XYSeries(name)
        for (i in 0..data.size-1)
            xy.add(i,data[i])

        var dataset = XYSeriesCollection()
        dataset.addSeries(xy)

        val chart = ChartFactory.createXYLineChart(
                "", // title
                name, // x-axis label
                "estimate", // y-axis label
                dataset // data
        )




        var panel = ChartPanel(chart)
        panel.fillZoomRectangle = true
        panel.isMouseWheelEnabled = true
        panel.preferredSize = java.awt.Dimension(300, 200)
        frame.contentPane.add(panel)
        frame.contentPane.repaint();
    }

}
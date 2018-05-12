package ru.efreet.trading

import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.axis.DateAxis
import org.jfree.chart.axis.NumberAxis
import org.jfree.chart.plot.ValueMarker
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.xy.StandardXYItemRenderer
import org.jfree.data.general.SeriesException
import org.jfree.data.time.Minute
import org.jfree.data.time.TimeSeriesCollection
import org.jfree.ui.ApplicationFrame
import org.jfree.ui.RefineryUtilities
import ru.efreet.trading.bars.checkBars
import ru.efreet.trading.bot.StatsCalculator
import ru.efreet.trading.bot.TradeHistory
import ru.efreet.trading.exchange.Exchange
import ru.efreet.trading.exchange.impl.cache.BarsCache
import ru.efreet.trading.logic.BotLogic
import ru.efreet.trading.logic.ProfitCalculator
import ru.efreet.trading.logic.impl.LogicFactory
import ru.efreet.trading.utils.CmdArgs
import ru.efreet.trading.utils.loadFromJson
import ru.efreet.trading.utils.toJson
import java.awt.Color
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*

/**
 * Created by fluder on 15/02/2018.
 */
class Graph {

    fun drawHistory(history: TradeHistory) {

        val stats = StatsCalculator().stats(history)

        for (trade in history.trades) {
            println("TRADE: ${trade}")
        }

        println("STATS:  ${stats}")

        val dataset = TimeSeriesCollection()

        for ((iName, iValues) in history.indicators) {
            if (iName.contains("macd", true) || iName.contains("sd", true))
                continue

            val ts = org.jfree.data.time.TimeSeries(iName)

            for (v in iValues) {
                try {
                    ts.add(Minute(Date.from(v.first.toInstant())), v.second)
                } catch (e: SeriesException) {
                    println("dup: $v")
                }
            }

            dataset.addSeries(ts)
        }

        val (startTime, startFunds) = history.cash.first()

        //время в часах
        val t = Duration.between(startTime, history.cash.last().first).toHours()

        //профит в час
        val k = Math.pow(history.cash.last().second / startFunds, 1.0 / t)


        val cashDataset = TimeSeriesCollection()
        val cashSeries = org.jfree.data.time.TimeSeries("usd")
        val idealSeries = org.jfree.data.time.TimeSeries("ideal")

        for (c in history.cash) {
            cashSeries.add(Minute(Date.from(c.first.toInstant())), c.second)
            idealSeries.add(Minute(Date.from(c.first.toInstant())), startFunds * Math.pow(k, Duration.between(startTime, c.first).toHours().toDouble()))
        }

        cashDataset.addSeries(cashSeries)
        cashDataset.addSeries(idealSeries)

        ///
//        val profitDataset = TimeSeriesCollection()
//
//            if (stats.profitStats !=null){
//                for ((s, l) in stats.profitStats){
//                    val profitSeries = org.jfree.data.time.TimeSeries(s)
//
//                    for (c in l) {
//                        profitSeries.addOrUpdate(Minute(Date.from(c.first.toInstant())), c.second)
//                    }
//
//                    profitDataset.addSeries(profitSeries)
//                }
//            }
        ///

        val chart = ChartFactory.createTimeSeriesChart(
                "poloniex", // title
                "Date", // x-axis label
                "Price Per Unit", // y-axis label
                dataset, // data
                true, // create legend?
                true, // generate tooltips?
                false // generate URLs?
        )
        val plot = chart.plot as XYPlot
        val axis = plot.domainAxis as DateAxis
        axis.dateFormatOverride = SimpleDateFormat("yyyy-MM-dd")


        // Adding markers to plot
        for (trade in history.trades) {
            // Buy signal

            val signalBarTime = Minute(Date.from(trade.time!!.toInstant())).firstMillisecond.toDouble()
            val marker = ValueMarker(signalBarTime)
            if (trade.decision == Decision.BUY) {
                marker.paint = Color.GREEN
                marker.label = "B" + trade.decisionArgs.toString()
            } else {
                marker.paint = Color.RED
                marker.label = "S" + trade.decisionArgs.toString()
            }
//                if (trade.profit != null)
//                    marker.label += "(${(trade.profit!! * 100).toInt()} / ${trade.price.toInt()} )"
            plot.addDomainMarker(marker)
        }

        /*if (history.indicators.containsKey("macdrule")) {
            var prevmacd: Double? = null
            for ((time, macd) in history.indicators["macdrule"]!!) {
                if (macd > 0 && (prevmacd == null || prevmacd < 0)) {
                    val signalBarTime = Minute(Date.from(time.toInstant())).firstMillisecond.toDouble()
                    val marker = ValueMarker(signalBarTime)
                    marker.paint = Color.ORANGE
                    marker.label = "MACD(B)"
                    plot.addDomainMarker(marker)
                } else if (macd < 0 && (prevmacd == null || prevmacd > 0)) {
                    val signalBarTime = Minute(Date.from(time.toInstant())).firstMillisecond.toDouble()
                    val marker = ValueMarker(signalBarTime)
                    marker.paint = Color.BLACK
                    marker.label = "MACD(S)"
                    plot.addDomainMarker(marker)
                }
                prevmacd = macd
            }
        }*/

        val cashAxis = NumberAxis("cash")
        cashAxis.autoRangeIncludesZero = false
        plot.setRangeAxis(1, cashAxis)
        plot.setDataset(1, cashDataset)
        plot.mapDatasetToRangeAxis(1, 1)
        val cashFlowRenderer = StandardXYItemRenderer()
        cashFlowRenderer.setSeriesPaint(0, Color.blue)
        plot.setRenderer(1, cashFlowRenderer)


//        val profitAxis = NumberAxis("profit")
//        profitAxis.autoRangeIncludesZero = false
//        plot.setRangeAxis(2, profitAxis)
//        plot.setDataset(2, profitDataset)
//        plot.mapDatasetToRangeAxis(2, 2)
//        val profitFlowRenderer = StandardXYItemRenderer()
//        profitFlowRenderer.setSeriesPaint(0, Color.MAGENTA)
//        plot.setRenderer(2, profitFlowRenderer)


        val panel = ChartPanel(chart)
        panel.fillZoomRectangle = true
        panel.isMouseWheelEnabled = true
        panel.preferredSize = java.awt.Dimension(500, 270)
        // Application frame
        val frame = ApplicationFrame("Ta4j example - Indicators to chart")
        frame.contentPane = panel
        frame.pack()
        RefineryUtilities.centerFrameOnScreen(frame)
        frame.isVisible = true
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            val cmd = CmdArgs.parse(args)

            val history: TradeHistory

            if (cmd.settings?.contains(".properties") == true) {

                val cache = BarsCache(cmd.cachePath)

                val exchange = Exchange.getExchange(cmd.exchange)

                val logic: BotLogic<Any> = LogicFactory.getLogic(cmd.logicName, cmd.instrument, cmd.barInterval)
                logic.loadState(cmd.settings!!)

                val historyStart = cmd.start!!.minus(cmd.barInterval.duration.multipliedBy(logic.historyBars))
                val bars = cache.getBars(exchange.getName(), cmd.instrument, cmd.barInterval, historyStart, cmd.end!!)
                bars.checkBars()

                val sp = logic.getParams()

                println(sp.toJson())

                history = ProfitCalculator().tradeHistory(cmd.logicName,
                        sp, cmd.instrument, cmd.barInterval, exchange.getFee(), bars,
                        listOf(Pair(cmd.start!!, ZonedDateTime.now())),
                        true)
            } else if (cmd.settings?.endsWith(".json") == true) {
                history = TradeHistory.loadFromJson(cmd.settings!!)
            } else {
                throw RuntimeException("invalid ${cmd.settings}")
            }

            Graph().drawHistory(history)
        }
    }
}
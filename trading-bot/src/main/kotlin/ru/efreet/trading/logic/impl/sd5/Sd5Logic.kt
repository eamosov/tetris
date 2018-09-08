package ru.efreet.trading.logic.impl.sd5

import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.logic.AbstractXExtBarBotLogic
import ru.efreet.trading.logic.impl.SimpleBotLogicParams
import ru.efreet.trading.logic.impl.sd3.Sd3Logic
import ru.efreet.trading.ta.indicators.*
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask

open class Sd5Logic(name: String, instrument: Instrument, barInterval: BarInterval) : Sd3Logic(name, instrument, barInterval) {

    override fun newInitParams(): SimpleBotLogicParams = SimpleBotLogicParams()

    init {
        describeCommomParams()
    }

    override fun prepareBars() {

        shortEma = XDoubleEMAIndicator(bars, XExtBar._shortEma1, XExtBar._shortEma2, XExtBar._shortEma, closePrice, params.short!!)
        longEma = XDoubleEMAIndicator(bars, XExtBar._longEma1, XExtBar._longEma2, XExtBar._longEma, closePrice, params.long!!)
        macd = XMACDIndicator(shortEma, longEma)
        signalEma = XDoubleEMAIndicator(bars, XExtBar._signalEma1, XExtBar._signalEma2, XExtBar._signalEma, macd, params.signal!!)
        signal2Ema = XDoubleEMAIndicator(bars, XExtBar._signal2Ema1, XExtBar._signal2Ema2, XExtBar._signal2Ema, macd, params.signal2!!)

        sma = XSMAIndicator(bars, XExtBar._sma, closePrice, params.deviationTimeFrame!!)
        sd = XStandardDeviationIndicator(bars, XExtBar._sd, closePrice, sma, params.deviationTimeFrame!!)

        dayShortEma = XEMAIndicator(bars, XExtBar._dayShortEma, closePrice, params.dayShort!!)
        dayLongEma = XEMAIndicator(bars, XExtBar._dayLongEma, closePrice, params.dayLong!!)
        dayMacd = XMACDIndicator(dayShortEma, dayLongEma)
        daySignalEma = XEMAIndicator(bars, XExtBar._daySignalEma, dayMacd, params.daySignal!!)
        daySignal2Ema = XEMAIndicator(bars, XExtBar._daySignal2Ema, dayMacd, params.daySignal2!!)
        lastDecisionIndicator = XLastDecisionIndicator(bars, XExtBar._lastDecision, { index, bar -> getTrendDecision(index, bar) })
        decisionStartIndicator = XDecisionStartIndicator(bars, XExtBar._decisionStart, lastDecisionIndicator)
        tslIndicator = XTslIndicator(bars, XExtBar._tslIndicator, lastDecisionIndicator, closePrice)
        soldBySLIndicator = XSoldBySLIndicator(bars, XExtBar._soldBySLIndicator, lastDecisionIndicator, tslIndicator, decisionStartIndicator, params.stopLoss, params.tStopLoss, params.takeProfit, params.tTakeProfit)

        val tasks = mutableListOf<ForkJoinTask<*>>()
        tasks.add(ForkJoinPool.commonPool().submit { shortEma.prepare() })
        tasks.add(ForkJoinPool.commonPool().submit { longEma.prepare() })
        tasks.add(ForkJoinPool.commonPool().submit { (sma as XCachedIndicator<*>).prepare() })
        tasks.add(ForkJoinPool.commonPool().submit { sd.prepare() })
        tasks.add(ForkJoinPool.commonPool().submit { dayShortEma.prepare() })
        tasks.add(ForkJoinPool.commonPool().submit { dayLongEma.prepare() })
        tasks.forEach { it.join() }

        tasks.clear()
        tasks.add(ForkJoinPool.commonPool().submit { signalEma.prepare() })
        tasks.add(ForkJoinPool.commonPool().submit { signal2Ema.prepare() })
        tasks.add(ForkJoinPool.commonPool().submit { daySignalEma.prepare() })
        tasks.add(ForkJoinPool.commonPool().submit { daySignal2Ema.prepare() })
        tasks.forEach { it.join() }

        soldBySLIndicator.prepare()
        barsIsPrepared = true
    }
}
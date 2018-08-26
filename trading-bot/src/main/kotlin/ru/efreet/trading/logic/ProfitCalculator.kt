package ru.efreet.trading.logic

import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.bot.TradeHistory
import ru.efreet.trading.bot.Trader
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.logic.impl.LogicFactory
import java.time.ZonedDateTime

/**
 * Created by fluder on 22/02/2018.
 */
class ProfitCalculator {

    fun <P : Any> tradeHistory(logicName: String,
                               params: P,
                               instrument: Instrument,
                               interval: BarInterval,
                               feeP: Float,
                               bars: List<XBar>,
                               times: List<Pair<ZonedDateTime, ZonedDateTime>>,
                               fillIndicators: Boolean): TradeHistory {

        val logic: BotLogic<P, XBar> = LogicFactory.getLogic(logicName, instrument, interval)
        bars.forEach { logic.insertBar(it) }
        logic.setParams(params)

        val trader = Trader.fakeTrader(feeP, interval, instrument)

        for (ti in times) {

            val startIndex = logic.getBarIndex(ti.first)

            for (index in startIndex until logic.barsCount()) {
                val advice = logic.getBotAdvice(index, fillIndicators)

                if (!advice.time.isBefore(ti.second)) {

//                    //В конце всегда всё продать
//                    if (trader.lastTrade()?.decision == Decision.BUY) {
//                        trader.executeAdvice(BotAdvice(ti.second.minusSeconds(1), Decision.SELL, mapOf(Pair("end", "true")), instrument, advice.price, trader.availableAsset(instrument), advice.bar, advice.indicators))
//                    }
                    break
                }

                trader.executeAdvice(advice)
            }

        }

        return trader.history()
    }

}
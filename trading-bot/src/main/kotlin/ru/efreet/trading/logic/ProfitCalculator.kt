package ru.efreet.trading.logic

import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.OrderSide
import ru.efreet.trading.logic.impl.LogicFactory
import ru.efreet.trading.bars.XBar
import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.bot.TradeHistory
import ru.efreet.trading.bot.Advice
import ru.efreet.trading.bot.FakeTrader
import ru.efreet.trading.bot.OrderSideExt
import java.time.ZonedDateTime

/**
 * Created by fluder on 22/02/2018.
 */
class ProfitCalculator {

    fun <P : AbstractBotLogicParams> tradeHistory(logicName: String,
                                                  params: P,
                                                  instrument: Instrument,
                                                  interval: BarInterval,
                                                  feeP: Double,
                                                  bars: List<XBar>,
                                                  times: List<Pair<ZonedDateTime, ZonedDateTime>>,
                                                  fillIndicators: Boolean): TradeHistory {

        val logic: BotLogic<P> = LogicFactory.getLogic(logicName, instrument, interval, XExtBar.of(bars))
        logic.setParams(params)

        val trader = FakeTrader(feeP = feeP, fillCash = fillIndicators, exchangeName = "", instrument = instrument);

        for (ti in times) {

            val startIndex = logic.getBarIndex(ti.first)

            for (index in startIndex until logic.barsCount()) {
                val advice = logic.getAdvice(index, null, trader, fillIndicators)

                if (!advice.time.isBefore(ti.second)) {
                    if (trader.lastTrade()?.side == OrderSide.BUY) {
                        trader.executeAdvice(Advice(ti.second.minusSeconds(1), OrderSideExt(OrderSide.SELL, false), false, false, null, instrument, advice.price, trader.availableAsset(instrument), advice.bar, advice.indicators))
                    }
                    break
                }

                trader.executeAdvice(advice)

            }

        }

        return trader.history(times.first().first, times.last().second)
    }

}
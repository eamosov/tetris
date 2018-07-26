package ru.efreet.trading.bot

import ru.efreet.trading.Decision
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.OrderType
import ru.efreet.trading.exchange.TradeRecord
import java.util.*

/**
 * Created by fluder on 23/02/2018.
 */
class FakeTrader(override val startUsd: Double = 1000.0,
                 val feeP: Double,
                 exchangeName: String) : AbstractTrader(exchangeName) {


    override var usd = startUsd

    override val instruments: Collection<Instrument> get() = tradeData.keys

    fun feeRatio(): Double {
        return feeP / 100.0 / 2.0
    }

    override fun executeAdvice(advice: BotAdvice): TradeRecord? {

        super.executeAdvice(advice)

        val td = tradeData(advice.instrument)

        if (advice.decision == Decision.BUY && advice.amount > 0) {

            if (advice.amount * advice.price >= 10) {
                val fee = advice.amount * feeRatio()
                val usdBefore = usd
                val assetBefore = td.asset
                usd -= advice.price * advice.amount
                td.asset += advice.amount - fee

                val trade = TradeRecord(UUID.randomUUID().toString(),
                        advice.time,
                        exchangeName,
                        advice.instrument.toString(),
                        advice.price, advice.decision,
                        advice.decisionArgs,
                        OrderType.LIMIT,
                        advice.amount,
                        feeRatio(),
                        usdBefore,
                        assetBefore,
                        usd,
                        td.asset)

                td.trades.add(trade)
                return trade
            }
        } else if (advice.decision == Decision.SELL && advice.amount > 0) {
            if (advice.amount * advice.price >= 10) {
                val fee = advice.amount * feeRatio()
                val usdBefore = usd
                val assetBefore = td.asset
                usd += advice.price * (advice.amount - fee)
                td.asset -= advice.amount
                val trade = TradeRecord(UUID.randomUUID().toString(),
                        advice.time,
                        exchangeName,
                        advice.instrument.toString(),
                        advice.price,
                        advice.decision,
                        advice.decisionArgs,
                        OrderType.LIMIT,
                        advice.amount,
                        feeRatio(),
                        usdBefore,
                        assetBefore,
                        usd,
                        td.asset)

                td.trades.add(trade)
                return trade
            }
        }

        return null
    }

    override fun availableAsset(instrument: Instrument): Double {
        return tradeData(instrument).asset
    }

    override fun price(instrument: Instrument): Double {
        return tradeData(instrument).endPrice
    }
}
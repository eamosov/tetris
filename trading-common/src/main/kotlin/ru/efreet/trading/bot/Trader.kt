package ru.efreet.trading.bot

import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.exchange.TradeRecord

/**
 * Created by fluder on 23/02/2018.
 */
interface Trader {

    fun availableUsd(instrument: Instrument): Double

    fun availableAsset(instrument: Instrument): Double

    fun executeAdvice(advice: Advice): TradeRecord?

    fun lastTrade(): TradeRecord?

    fun history(): TradeHistory
}
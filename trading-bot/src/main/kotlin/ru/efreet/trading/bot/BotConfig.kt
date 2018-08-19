package ru.efreet.trading.bot

import ru.efreet.trading.exchange.Instrument

data class BotConfig(val instruments: Map<Instrument, Double> = mapOf(Pair(Instrument.ETH_USDT, 0.5)),
                     val usdLimit:Double = 0.1,
                     val telegram:Boolean=false,
                     val keepBnb:Double = 1.0) {

}
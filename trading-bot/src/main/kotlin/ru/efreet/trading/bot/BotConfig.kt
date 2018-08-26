package ru.efreet.trading.bot

import ru.efreet.trading.exchange.Instrument

data class BotConfig(val instruments: Map<Instrument, Float> = mapOf(Pair(Instrument.ETH_USDT, 0.5F)),
                     val usdLimit:Float = 0.1F,
                     val telegram:Boolean=false,
                     val keepBnb:Float = 1.0F) {

}
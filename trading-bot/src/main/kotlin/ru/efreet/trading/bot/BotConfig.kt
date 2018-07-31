package ru.efreet.trading.bot

import ru.efreet.trading.exchange.Instrument

data class BotConfig(val instruments: List<Instrument> = listOf(Instrument.ETH_USDT), val usdLimit:Double = 0.1, val betLimit:Double = 0.5, val telegram:Boolean=false) {

}
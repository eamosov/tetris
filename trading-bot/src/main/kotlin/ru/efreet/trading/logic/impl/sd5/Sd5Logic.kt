package ru.efreet.trading.logic.impl.sd5

import ru.efreet.trading.Decision
import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.bot.BotAdvice
import ru.efreet.trading.bot.Trader
import ru.efreet.trading.bot.TradesStats
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.logic.AbstractBotLogic
import ru.efreet.trading.logic.BotLogic
import ru.efreet.trading.logic.impl.SimpleBotLogicParams
import ru.efreet.trading.logic.impl.sd3.Sd3Logic
import ru.efreet.trading.ta.indicators.*
import ru.efreet.trading.trainer.Metrica
import java.time.Duration
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask

open class Sd5Logic(name: String, instrument: Instrument, barInterval: BarInterval, bars: MutableList<XExtBar>)  : Sd3Logic(name, instrument, barInterval, bars) {

}
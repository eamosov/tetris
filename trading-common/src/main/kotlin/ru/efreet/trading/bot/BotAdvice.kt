package ru.efreet.trading.bot

import ru.efreet.trading.Decision
import ru.efreet.trading.bars.XBar
import ru.efreet.trading.exchange.Instrument
import java.time.ZonedDateTime

/**
 * Created by fluder on 25/02/2018.
 */
data class BotAdvice(val time: ZonedDateTime,
                     val decision: Decision,
                     val decisionArgs: Map<String, String>,
                     val instrument: Instrument,
                     val price: Double,
                     val bar: XBar,
                     val indicators: Map<String, Double>?)

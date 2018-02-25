package ru.efreet.trading.logic.impl

import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.logic.BotLogic
import ru.efreet.trading.logic.impl.macd.MacdLogic
import ru.efreet.trading.logic.impl.sd.SdLogic
import ru.efreet.trading.logic.impl.sd2.Sd2Logic
import ru.efreet.trading.logic.impl.sd3.Sd3Logic
import ru.efreet.trading.logic.impl.sdonly.SdOnlyLogic
import ru.efreet.trading.bars.XExtBar

/**
 * Created by fluder on 21/02/2018.
 */
class LogicFactory {
    companion object {
        fun <P> getLogic(name: String, instrument: Instrument, barInterval: BarInterval, bars: MutableList<XExtBar> = mutableListOf()): BotLogic<P> {
            return when (name) {
                "macd" -> return MacdLogic("macd", instrument, barInterval, bars) as BotLogic<P>
                "sd" -> return SdLogic("sd", instrument, barInterval, bars) as BotLogic<P>
                "sd2" -> return Sd2Logic("sd2", instrument, barInterval, bars) as BotLogic<P>
                "sd3" -> return Sd3Logic("sd3", instrument, barInterval, bars) as BotLogic<P>
                "sdonly" -> return SdOnlyLogic("sdonly", instrument, barInterval, bars) as BotLogic<P>
                else -> throw RuntimeException("Unknown logic ${name}")
            }

        }
    }
}
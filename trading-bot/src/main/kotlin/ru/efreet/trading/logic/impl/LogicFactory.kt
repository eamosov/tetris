package ru.efreet.trading.logic.impl

import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.logic.BotLogic
import ru.efreet.trading.logic.impl.sd3.Sd3Logic
import ru.efreet.trading.logic.impl.sd4.Sd4Logic
import ru.efreet.trading.logic.impl.sd5.Sd5Logic

/**
 * Created by fluder on 21/02/2018.
 */
class LogicFactory {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <P> getLogic(name: String, instrument: Instrument, barInterval: BarInterval, bars: MutableList<XExtBar> = mutableListOf()): BotLogic<P> =
                when (name) {
                    "sd3" -> Sd3Logic("sd3", instrument, barInterval, bars) as BotLogic<P>
                    "sd4" -> Sd4Logic("sd4", instrument, barInterval, bars) as BotLogic<P>
                    "sd5" -> Sd5Logic("sd5", instrument, barInterval, bars) as BotLogic<P>
                    "gustos2" -> Class.forName("ru.gustos.trading.GustosBotLogic2").constructors[0].newInstance("gustos2", instrument, barInterval, bars) as BotLogic<P>
                    "gustostest" -> Class.forName("ru.gustos.trading.GustosBotLogic2WithMacd").constructors[0].newInstance("gustos2", instrument, barInterval, bars) as BotLogic<P>
                    else -> throw RuntimeException("Unknown logic ${name}")
                }
    }
}
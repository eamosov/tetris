package ru.efreet.trading.logic.impl

import ru.efreet.trading.bars.XBar
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
        @JvmStatic
        fun <P, B:XBar> getLogic(name: String, instrument: Instrument, barInterval: BarInterval, simulate: Boolean = false): BotLogic<P, B> =
                when (name) {
                    "sd3" -> Sd3Logic("sd3", instrument, barInterval) as BotLogic<P, B>
                    "sd4" -> Sd4Logic("sd4", instrument, barInterval) as BotLogic<P, B>
                    "sd5" -> Sd5Logic("sd5", instrument, barInterval) as BotLogic<P, B>
                    "gustos2" -> Class.forName("ru.gustos.trading.GustosBotLogic2").constructors[0].newInstance("gustos2", instrument, barInterval) as BotLogic<P, B>
                    "gustostest" -> Class.forName("ru.gustos.trading.GustosBotLogicTest2").constructors[0].newInstance("gustostest2", instrument, barInterval) as BotLogic<P, B>
                    "levels" -> Class.forName("ru.gustos.trading.LevelsBotLogic").constructors[0].newInstance("levels", instrument, barInterval) as BotLogic<P, B>
                    "ml" -> Class.forName("ru.gustos.trading.GustosBotLogic3").constructors[0].newInstance("ml", instrument, barInterval, simulate) as BotLogic<P, B>
                    else -> throw RuntimeException("Unknown logic ${name}")
                }
    }
}
package ru.efreet.trading.logic.impl.sd4

import ru.efreet.trading.bars.XExtBar
import ru.efreet.trading.bot.TradesStats
import ru.efreet.trading.exchange.BarInterval
import ru.efreet.trading.exchange.Instrument
import ru.efreet.trading.logic.impl.SimpleBotLogicParams
import ru.efreet.trading.logic.impl.sd3.Sd3Logic
import ru.efreet.trading.trainer.Metrica

/**
 * Created by fluder on 20/02/2018.
 */
class Sd4Logic(name: String, instrument: Instrument, barInterval: BarInterval, bars: MutableList<XExtBar>) : Sd3Logic(name, instrument, barInterval, bars) {

    override fun metrica(params: SimpleBotLogicParams, stats: TradesStats): Metrica {
        return super.metrica(params, stats).add("trades", stats.trades * 0.0175)
    }

}
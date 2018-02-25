package ru.efreet.trading.logic.impl

import ru.efreet.trading.logic.AbstractBotLogicParams

/**
 * Created by fluder on 20/02/2018.
 */
data class SimpleBotLogicParams(var short: Int? = null,
                                var long: Int? = null,
                                var signal: Int? = null,
                                var deviationTimeFrame: Int? = null,
                                var deviation: Int? = null,

                                var dayShort: Int? = 712,
                                var dayLong: Int? = 1487,
                                var daySignal: Int? = 1433,

                                override var stopLoss: Double = 10.0) : AbstractBotLogicParams {

}
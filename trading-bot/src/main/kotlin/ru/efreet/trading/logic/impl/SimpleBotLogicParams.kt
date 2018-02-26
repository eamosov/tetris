package ru.efreet.trading.logic.impl

import ru.efreet.trading.logic.AbstractBotLogicParams

/**
 * Created by fluder on 20/02/2018.
 */
data class SimpleBotLogicParams(var short: Int? = 1,
                                var long: Int? = 1,
                                var signal: Int? = 1,
                                var deviationTimeFrame: Int? = 1,
                                var deviation: Int? = 1,

                                var dayShort: Int? = 712,
                                var dayLong: Int? = 1487,
                                var daySignal: Int? = 1433,

                                var f3Index: Int? = null,

                                override var stopLoss: Double = 10.0) : AbstractBotLogicParams {

}
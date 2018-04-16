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

                                var dayShort: Int? = null,
                                var dayLong: Int? = null,
                                var daySignal: Int? = null,
                                var daySignal2: Int? = null,

                                override var stopLoss: Double = 10.0,
                                override var tStopLoss: Double = 1.5,

                                var persist1: Int? = 4,
                                var persist2: Int? = 8,
                                var persist3: Int? = 1) : AbstractBotLogicParams {

}
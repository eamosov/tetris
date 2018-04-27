package ru.efreet.trading.logic.impl

import ru.efreet.trading.logic.AbstractBotLogicParams

/**
 * Created by fluder on 20/02/2018.
 */
data class SimpleBotLogicParams(var short: Int? = 1,
                                var long: Int? = null,
                                var signal: Int? = null,
                                var signal2: Int? = null,
                                var deviationTimeFrame: Int? = 100,
                                var deviationTimeFrame2: Int? = 241,
                                var deviation: Int? = 2,
                                var deviation2: Int? = 8,

                                var dayShort: Int? = 38,
                                var dayLong: Int? = null,
                                var daySignal: Int? = null,
                                var daySignal2: Int? = null,

                                override var stopLoss: Double = 10.0,
                                override var tStopLoss: Double = 50.0,

                                override var takeProfit: Double = 100.0,
                                override var tTakeProfit: Double = 0.1,

                                var persist1: Int? = 119,
                                var persist2: Int? = 21,
                                var persist3: Int? = 92) : AbstractBotLogicParams {

}
package ru.efreet.trading.logic.impl

import ru.efreet.trading.logic.AbstractBotLogicParams

/**
 * Created by fluder on 20/02/2018.
 */
data class SimpleBotLogicParams(var short: Int? = 1,
                                var long: Int? = null,
                                var signal: Int? = null,
                                var signal2: Int? = null,
                                var deviationTimeFrame: Int? = 26,
                                var deviation: Int? = 17,
                                var deviation2: Int? = 14,

                                var dayShort: Int? = 240,
                                var dayLong: Int? = null,
                                var daySignal: Int? = null,
                                var daySignal2: Int? = null,

                                override var stopLoss: Double = 2.8,
                                override var tStopLoss: Double = 50.0,

                                override var takeProfit: Double = 100.0,
                                override var tTakeProfit: Double = 0.1,

                                var persist1: Int? = 30,
                                var persist2: Int? = 2,
                                var persist3: Int? = 0) : AbstractBotLogicParams {

}
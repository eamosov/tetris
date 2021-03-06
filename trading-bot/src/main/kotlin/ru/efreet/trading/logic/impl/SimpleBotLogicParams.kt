package ru.efreet.trading.logic.impl

import ru.efreet.trading.logic.AbstractBotLogicParams

/**
 * Created by fluder on 20/02/2018.
 */
data class SimpleBotLogicParams(var short: Int? = 5,
                                var long: Int? = null,
                                var signal: Int? = null,
                                var signal2: Int? = null,
                                var deviationTimeFrame: Int? = null,
                                var deviationTimeFrame2: Int? = null,
                                var deviation: Int? = null,
                                var deviation2: Int? = null,

                                var dayShort: Int? = null,
                                var dayLong: Int? = null,
                                var daySignal: Int? = null,
                                var daySignal2: Int? = null,

                                override var stopLoss: Float = 2.5F,
                                override var tStopLoss: Float = 50.0F,

                                override var takeProfit: Float = 100.0F,
                                override var tTakeProfit: Float = 0.1F,

                                var persist1: Int? = 0,
                                var persist2: Int? = 0,
                                var persist3: Int? = 0) : AbstractBotLogicParams {

}
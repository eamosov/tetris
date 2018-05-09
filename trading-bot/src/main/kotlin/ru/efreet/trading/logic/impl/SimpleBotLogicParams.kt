package ru.efreet.trading.logic.impl

import ru.efreet.trading.logic.AbstractBotLogicParams

/**
 * Created by fluder on 20/02/2018.
 */
data class SimpleBotLogicParams(var short: Int? = 5,
                                var long: Int? = null,
                                var signal: Int? = null,
                                var signal2: Int? = null,
                                var deviationTimeFrame: Int? = 619,
                                var deviationTimeFrame2: Int? = 1066,
                                var deviationTimeFrame3: Int? = 619,
                                var deviationTimeFrameSell: Int? = 619,
                                var deviationTimeFrameSell2: Int? = 1066,
                                var deviationTimeFrameSell3: Int? = 619,
                                var deviation: Int? = 20,
                                var deviation2: Int? = 15,
                                var deviation3: Int? = 10,

                                var dayShort: Int? = 90,
                                var dayLong: Int? = null,
                                var daySignal: Int? = null,
                                var daySignal2: Int? = null,

                                override var stopLoss: Double = 2.5,
                                override var tStopLoss: Double = 50.0,

                                override var takeProfit: Double = 100.0,
                                override var tTakeProfit: Double = 0.1,

                                var persist1: Int? = 24,
                                var persist2: Int? = 14,
                                var persist3: Int? = 0) : AbstractBotLogicParams {

}
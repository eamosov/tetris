package ru.gustos.trading

import ru.efreet.trading.logic.AbstractBotLogicParams

data class GustosBotLogicParams(override var stopLoss: Double = 2.5,
                                override var tStopLoss: Double = 50.0,

                                override var takeProfit: Double = 100.0,
                                override var tTakeProfit: Double = 0.1,

                                var deviation: Int? = 2,
                                var deviation2: Int? = 2,
                                var deviationTimeFrame: Int? = 50,
                                var deviationTimeFrame2: Int? = 120,

                                var persist1: Int? = 0,
                                var persist2: Int? = 0,
                                var persist3: Int? = 0) : AbstractBotLogicParams {

}
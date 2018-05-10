package ru.gustos.trading

import ru.efreet.trading.logic.AbstractBotLogicParams

data class GustosBotLogicParams(var volumeShort: Int? = null,
                                var buyWindow: Int? = null,
                                var buyVolumeWindow: Int? = null,
                                var sellWindow: Int? = null,
                                var sellVolumeWindow: Int? = null,
                                var buyDiv: Int? = null,
                                var sellDiv: Int? = null,
                                var buyBoundDiv: Int? = null,
                                var sellBoundDiv: Int? = null,

                                override var stopLoss: Double = 2.5,
                                override var tStopLoss: Double = 50.0,

                                override var takeProfit: Double = 100.0,
                                override var tTakeProfit: Double = 0.1

                                ) : AbstractBotLogicParams {

}
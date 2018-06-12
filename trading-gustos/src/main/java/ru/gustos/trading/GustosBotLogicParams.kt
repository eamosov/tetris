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
                                var volumePow1: Int? = 24,
                                var volumePow2: Int? = 14,

                                override var stopLoss: Double = 2.5,
                                override var tStopLoss: Double = 50.0,

                                override var takeProfit: Double = 100.0,
                                override var tTakeProfit: Double = 0.1

                                ) : AbstractBotLogicParams {
    fun copyIt() : GustosBotLogicParams = copy()

    fun getVolumePow1Sq() : Int =volumePow1!!*volumePow1!!
    fun getVolumePow2Sq() : Int =volumePow2!!*volumePow2!!
    fun getBuyWindowSq() : Int =buyWindow!!*buyWindow!!
    fun getSellWindowSq() : Int =sellWindow!!*sellWindow!!

    fun getSellBoundDivSq() : Int =sellBoundDiv!!*sellBoundDiv!!
    fun getBuyDivSq() : Int =buyDiv!!*buyDiv!!
    fun getSellDivSq() : Int =sellDiv!!*sellDiv!!
    fun getVolumeShortSq() : Int =volumeShort!!*volumeShort!!

    fun getVolumePow1Sq2() : Int =volumePow1!!*volumePow1!!*volumePow1!!
    fun getVolumePow2Sq2() : Int =volumePow2!!*volumePow2!!*volumePow2!!
    fun getBuyWindowSq2() : Int =buyWindow!!*buyWindow!!*buyWindow!!
    fun getSellWindowSq2() : Int =sellWindow!!*sellWindow!!*sellWindow!!

    fun getSellBoundDivSq2() : Int =sellBoundDiv!!*sellBoundDiv!!*sellBoundDiv!!
    fun getBuyDivSq2() : Int =buyDiv!!*buyDiv!!*buyDiv!!
    fun getSellDivSq2() : Int =sellDiv!!*sellDiv!!*sellDiv!!
    fun getVolumeShortSq2() : Int =volumeShort!!*volumeShort!!*volumeShort!!
}

data class GustosBotLogicParams2(
                                var volumeShort: Int? = null,
                                var buyWindow: Int? = null,
                                var buyVolumeWindow: Int? = null,
                                var sellWindow: Int? = null,
                                var sellVolumeWindow: Int? = null,
                                var buyDiv: Int? = null,
                                var sellDiv: Int? = null,
                                var buyBoundDiv: Int? = null,
                                var sellBoundDiv: Int? = null,

                                var volumeShort2: Int? = null,
                                var buyWindow2: Int? = null,
                                var buyVolumeWindow2: Int? = null,
                                var sellWindow2: Int? = null,
                                var sellVolumeWindow2: Int? = null,
                                var buyDiv2: Int? = null,
                                var sellDiv2: Int? = null,
                                var buyBoundDiv2: Int? = null,
                                var sellBoundDiv2: Int? = null,

                                var macdShort: Int? = null,
                                var macdLong: Int? = null,
                                var macdSignal: Int? = null,


                                override var stopLoss: Double = 2.5,
                                override var tStopLoss: Double = 50.0,

                                override var takeProfit: Double = 100.0,
                                override var tTakeProfit: Double = 0.1

) : AbstractBotLogicParams {

}

data class LevelsLogicParams(
                                var descendK : Int? = null,
                                var substK : Int? = null,
                                var fPow : Int? = null,

                                var fixTime : Int? = null,
                                var fixAmp: Int? = null,

                                var diviation: Int? = null,

                                var lowVolumeK: Int? = null,
                                var highVolumeK: Int? = null,

                                var sellSdTimeFrame: Int? = null,


                                override var stopLoss: Double = 2.5,
                                override var tStopLoss: Double = 50.0,

                                override var takeProfit: Double = 100.0,
                                override var tTakeProfit: Double = 0.1

) : AbstractBotLogicParams {

}
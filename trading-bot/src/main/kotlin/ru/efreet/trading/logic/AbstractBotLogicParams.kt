package ru.efreet.trading.logic

/**
 * Created by fluder on 20/02/2018.
 */
interface AbstractBotLogicParams {
    var stopLoss: Double
    var tStopLoss: Double
    var takeProfit: Double
    var tTakeProfit: Double
}
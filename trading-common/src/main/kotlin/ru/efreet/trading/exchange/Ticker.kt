package ru.efreet.trading.exchange

/**
 * Created by fluder on 09/02/2018.
 */
data class Ticker(var instrument: Instrument, var highestBid:Double, var lowestAsk:Double)
package ru.efreet.trading.ta.indicators

/**
 * Created by fluder on 19/02/2018.
 */
interface XIndicator<B> {
    fun getValue(index:Int):Double
}
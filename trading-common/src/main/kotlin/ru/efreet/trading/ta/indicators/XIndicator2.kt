package ru.efreet.trading.ta.indicators

/**
 * Created by fluder on 19/02/2018.
 */
interface XIndicator2<B,V> {
    fun getValue(index:Int):V?
}
package ru.efreet.trading.ta.indicators

/**
 * Created by fluder on 19/02/2018.
 */
class XEMAIndicator<B>(bars: List<B>,
                       prop: BarGetterSetter<B>,
                       indicator: XIndicator<B>,
                       timeFrame: Int) : AbstractEMAIndicator<B>(bars, prop, indicator, timeFrame, 2.0 / (timeFrame + 1.0)) {

}
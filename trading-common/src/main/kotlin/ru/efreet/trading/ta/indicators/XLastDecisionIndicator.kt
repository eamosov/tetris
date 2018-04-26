package ru.efreet.trading.ta.indicators

import ru.efreet.trading.Decision

/**
 * Created by fluder on 17/03/2018.
 * Индикатор, возвращающий ближайший Bar, у которого  Decision != NONE
 */
class XLastDecisionIndicator<B>(bars: List<B>, prop: BarGetterSetter2<B, Pair<Decision, Map<String, String>>>, val trend: (index: Int, bar: B) -> Pair<Decision, Map<String, String>>) : XCachedIndicator2<B, Pair<Decision, Map<String, String>>>(bars, prop) {

    override fun calculate(index: Int, bar: B): Pair<Decision, Map<String, String>> {

        val s = trend(index, bar)
        return when {
            s.first != Decision.NONE -> s
            index == 0 -> Pair(Decision.SELL, emptyMap())
            else -> getValue(index - 1)
        }

    }
}
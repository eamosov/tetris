package ru.efreet.trading.test;

import ru.efreet.trading.Decision;
import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Instrument;
import ru.efreet.trading.exchange.impl.cache.BarsCache;
import ru.efreet.trading.logic.impl.sd3.Sd3Logic;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Created by fluder on 05/04/2018.
 */
public class Sd3LogicIndicatorTest {

    public static void main(String[] args) {

        BarsCache cache = new BarsCache("d:\\tetrislibs\\bars");

        final List<? extends XBar> bars = cache.getBars("binance", Instrument.getBTC_USDT(),
                BarInterval.ONE_MIN,
                ZonedDateTime.parse("2018-01-16T00:00:00Z[GMT]"),
                ZonedDateTime.now());

        final Sd3Logic logic = new Sd3Logic("sd3", Instrument.getBTC_USDT(), BarInterval.ONE_MIN);

        bars.forEach(bar -> logic.insertBar(bar, null));
        logic.prepareBars();

        double money = 1000;
        double btc = 0;
        Decision prev = Decision.SELL;
        for (int i = 0; i < logic.barsCount(); i++) {
            Decision side = logic.getAdvice(i, false).getDecision();
            if (side != prev) {
                if (side == Decision.BUY) {
                    btc += money / logic.getBar(i).getClosePrice() * 0.9995;
                    money = 0;
                } else if (side == Decision.SELL) {
                    money += btc * logic.getBar(i).getClosePrice() * 0.9995;
                    btc = 0;
                }

                System.out.println(String.format("%d %s: %g %s   %g",
                        i,
                        logic.getBar(i).getEndTime(),
                        logic.getBar(i).getClosePrice(), side, money));

            }
            prev = side;
        }
    }
}

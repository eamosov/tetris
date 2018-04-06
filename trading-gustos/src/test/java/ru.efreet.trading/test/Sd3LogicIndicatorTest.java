package ru.efreet.trading.test;

import ru.efreet.trading.bars.XBaseBar;
import ru.efreet.trading.bars.XExtBar;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Instrument;
import ru.efreet.trading.exchange.OrderSide;
import ru.efreet.trading.exchange.impl.cache.BarsCache;
import ru.efreet.trading.logic.BotLogic;
import ru.efreet.trading.logic.impl.sd3.Sd3Logic;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Created by fluder on 05/04/2018.
 */
public class Sd3LogicIndicatorTest {

    public static void main(String[] args) {

        BarsCache cache = new BarsCache("d:\\tetrislibs\\bars");

        final List<XBaseBar> bars = cache.getBars("binance", Instrument.getBTC_USDT(),
                                                  BarInterval.ONE_MIN,
                                                  ZonedDateTime.parse("2018-01-16T00:00:00Z[GMT]"),
                                                  ZonedDateTime.now());

        final BotLogic logic = new Sd3Logic("sd3", Instrument.Companion.getBTC_USDT(),
                                            BarInterval.ONE_MIN, XExtBar.Companion.of(bars));
        logic.prepare();

        double money = 1000;
        double btc = 0;
        OrderSide prev = OrderSide.SELL;
        for (int i = 0; i < logic.barsCount(); i++) {
            OrderSide side = logic.getAdvice(i, null, null, false).getOrderSide().getSide();
            if (side!=prev) {
                if (side==OrderSide.BUY) {
                    btc+=money/logic.getBar(i).getClosePrice()*0.9975;
                    money = 0;
                } else {
                    money+=btc*logic.getBar(i).getClosePrice()*0.9975;
                    btc = 0;
                }

                            System.out.println(String.format("%d %s: %g %s   %g",
                                    i,
                                    logic.getBar(i).getEndTime(),
                                    logic.getBar(i).getClosePrice(), side,money));

            }
            prev = side;
        }
    }
}

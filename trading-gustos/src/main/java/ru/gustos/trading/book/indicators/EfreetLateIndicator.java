package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XExtBar;
import ru.efreet.trading.bot.OrderSideExt;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Instrument;
import ru.efreet.trading.exchange.OrderSide;
import ru.efreet.trading.logic.BotLogic;
import ru.efreet.trading.logic.impl.sd3.Sd3Logic;
import ru.gustos.trading.book.Sheet;

import java.awt.*;
import java.util.stream.Collectors;

public class EfreetLateIndicator implements IIndicator {
    public static final int Id = 101;

    @Override
    public int getId() {
        return Id;
    }

    @Override
    public String getName() {
        return "efreetlate";
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.YESNO;
    }

    @Override
    public Color getColorMax() {
        return Color.green;
    }

    @Override
    public Color getColorMin() {
        return Color.red;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {

        final BotLogic logic = new Sd3Logic("sd3", Instrument.Companion.getBTC_USDT(),
                                            BarInterval.ONE_MIN, sheet.moments.stream()
                                                                              .map(m -> new XExtBar(m.bar))
                                                                              .collect(Collectors.toList()));
        logic.prepare();

        OrderSideExt prev= logic.getAdvice(0, null, null, false).getOrderSide();
        int buyAt = -1;
        double vv = 0;
        for (int i = 0; i < values.length; i++) {

            final OrderSideExt ose = logic.getAdvice(i, null, null, false).getOrderSide();

            if (ose!=null && ose.getSide()==OrderSide.BUY && vv==0) {
                if (buyAt<0)
                    buyAt = i+20;
                else if (buyAt == i) {
                    vv = 1;
                    buyAt = -1;
                }
            }

            if (ose!=null && ose.getSide()==OrderSide.SELL) {
                vv = 0;
                buyAt = -1;
            }

            values[i] = vv;
        }

    }
}

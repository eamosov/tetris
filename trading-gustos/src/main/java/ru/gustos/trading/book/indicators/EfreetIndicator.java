package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XExtBar;
import ru.efreet.trading.bot.OrderSideExt;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Instrument;
import ru.efreet.trading.exchange.OrderSide;
import ru.efreet.trading.logic.BotLogic;
import ru.efreet.trading.logic.impl.sd3.Sd3Logic;
import ru.gustos.trading.book.Sheet;

import java.awt.Color;
import java.util.stream.Collectors;

public class EfreetIndicator implements IIndicator {
    public static final int Id = 5;

    @Override
    public int getId() {
        return Id;
    }

    @Override
    public String getName() {
        return "efreet";
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

        for (int i = 0; i < values.length; i++) {

            final OrderSideExt ose = logic.getAdvice(i, null, null, false).getOrderSide();
            final Decision decision = (ose == null) ? Decision.NONE : ((ose.getSide() == OrderSide.BUY) ? Decision.BUY : Decision.SELL);

            values[i] = decision == Decision.BUY ? IIndicator.YES : (decision == Decision.SELL ? IIndicator.NO : Double.NaN);
        }

    }
}

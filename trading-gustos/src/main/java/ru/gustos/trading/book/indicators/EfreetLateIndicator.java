package ru.gustos.trading.book.indicators;

import ru.efreet.trading.Decision;
import ru.efreet.trading.bars.XExtBar;
import ru.efreet.trading.bot.BotAdvice;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Instrument;
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
        logic.loadState("sd3_2018_01_16.properties");
        logic.prepare();

        final BotAdvice prev= logic.getBotAdvice(0, null, null, false);
        int buyAt = -1;
        double vv = 0;
        for (int i = 0; i < values.length; i++) {

            final BotAdvice ose = logic.getBotAdvice(i, null, null, false);

            if (ose.getDecision()== Decision.BUY && vv==0) {
                if (buyAt<0)
                    buyAt = i+20;
                else if (buyAt == i) {
                    vv = 1;
                    buyAt = -1;
                }
            }

            if (ose.getDecision()==Decision.SELL) {
                vv = 0;
                buyAt = -1;
            }

            values[i] = vv;
        }

    }
}

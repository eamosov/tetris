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

public class EfreetSuccessIndicator implements IIndicator {
    public static final int Id = 9;

    @Override
    public int getId() {
        return Id;
    }

    @Override
    public String getName() {
        return "efreetSuc";
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

        double buyPrice = 0;
        int buyIndex = 0;
        BotAdvice prev= logic.getBotAdvice(0, null, null, false);
        for (int i = 0; i < values.length; i++) {

            final BotAdvice ose = logic.getBotAdvice(i, null, null, false);
            if (ose.getDecision()!=prev.getDecision()) {
                if (ose.getDecision() == Decision.BUY){
                    buyPrice = sheet.moments.get(i).bar.getClosePrice();
                    buyIndex = i;
                } else {
                    values[buyIndex] = sheet.moments.get(i).bar.getClosePrice()*0.999>buyPrice?IIndicator.YES:IIndicator.NO;
                }


                prev = ose;
            }else
                values[i] = 0;
        }

    }
}

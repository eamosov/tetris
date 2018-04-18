package ru.gustos.trading.book.indicators;

import ru.efreet.trading.Decision;
import ru.efreet.trading.bars.XExtBar;
import ru.efreet.trading.bot.BotAdvice;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Instrument;
import ru.efreet.trading.logic.BotLogic;
import ru.efreet.trading.logic.impl.LogicFactory;
import ru.gustos.trading.book.Sheet;

import java.awt.Color;
import java.util.stream.Collectors;

public class EfreetSd4Indicator extends BaseIndicator {
    public static int Id;

    public EfreetSd4Indicator(IndicatorInitData data) {
        super(data);
        Id = data.id;
    }

    @Override
    public String getName() {
        return "efreetSd4";
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

        final BotLogic logic = LogicFactory.Companion.getLogic("sd4",
                                                               Instrument.Companion.getBTC_USDT(),
                                                               BarInterval.ONE_MIN,
                                                               sheet.moments.stream()
                                                                            .map(m -> new XExtBar(m.bar))
                                                                            .collect(Collectors.toList()));

        logic.loadState("sd4_2018_01_16.properties");
        logic.prepare();

        for (int i = 0; i < values.length; i++) {

            final BotAdvice ose = logic.getBotAdvice(i, null, null, false);

            if (ose.getDecision() == Decision.BUY) {
                values[i] = IIndicator.YES;
            } else {
                values[i] = IIndicator.NO;
            }
        }

    }
}


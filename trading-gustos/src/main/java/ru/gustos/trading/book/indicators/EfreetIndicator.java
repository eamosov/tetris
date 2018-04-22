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

public class EfreetIndicator extends BaseIndicator {
    public static int Id;
    String logic;
    String state;

    public EfreetIndicator(IndicatorInitData data) {
        super(data);
        Id = data.id;
        logic = data.logic;
        state = data.state;
    }

    @Override
    public String getName() {
        return "efreet"+logic;
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

        final BotLogic logic = LogicFactory.Companion.getLogic(this.logic,
                                                               Instrument.Companion.getBTC_USDT(),
                                                               BarInterval.ONE_MIN,
                                                               sheet.moments.stream()
                                                                            .map(m -> new XExtBar(m.bar))
                                                                            .collect(Collectors.toList()));

        logic.loadState(state);
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



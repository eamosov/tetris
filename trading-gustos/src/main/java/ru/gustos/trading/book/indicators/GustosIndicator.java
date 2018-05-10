package ru.gustos.trading.book.indicators;

import ru.efreet.trading.Decision;
import ru.efreet.trading.bars.XExtBar;
import ru.efreet.trading.bot.BotAdvice;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Instrument;
import ru.efreet.trading.logic.BotLogic;
import ru.efreet.trading.logic.impl.LogicFactory;
import ru.gustos.trading.book.Sheet;

import java.awt.*;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class GustosIndicator extends BaseIndicator implements IIndicatorWithProperties{
    public static int Id;
    String logic;
    String state;
    BotLogic botLogic;

    public GustosIndicator(IndicatorInitData data) {
        super(data);
        Id = data.id;
        logic = data.logic;
        state = data.state;
    }

    @Override
    public String getName() {
        return "gustos"+logic;
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
    public Map<String,String> getMark(int ind) {
        final BotAdvice ose = botLogic.getBotAdvice(ind, null, null, true);
        return ose.getDecisionArgs();
    }

    @Override
    public void calcValues(Sheet sheet, double[] values, int from, int to) {

        if (botLogic==null) {
            botLogic = LogicFactory.Companion.getLogic(this.logic,
                    Instrument.Companion.getBTC_USDT(),
                    BarInterval.ONE_MIN,
                    sheet.moments.stream()
                            .map(m -> new XExtBar(m.bar))
                            .collect(Collectors.toList()));

            botLogic.loadState(state);
        }

        for (int i = from; i < to; i++) {

            final BotAdvice ose = botLogic.getBotAdvice(i, null, null, true);
            values[i] = ose.getDecision() == Decision.BUY ? IIndicator.YES : IIndicator.NO;

        }

    }

    @Override
    public Properties getIndicatorProperties() {
        return botLogic.getParamsAsProperties();
    }

    @Override
    public void setIndicatorProperties(Properties p) {
        botLogic.setParams(p);
    }
}

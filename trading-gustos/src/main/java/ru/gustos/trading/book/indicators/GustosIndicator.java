package ru.gustos.trading.book.indicators;

import ru.efreet.trading.Decision;
import ru.efreet.trading.bars.XExtBar;
import ru.efreet.trading.bot.BotAdvice;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Instrument;
import ru.efreet.trading.logic.BotLogic;
import ru.efreet.trading.logic.impl.LogicFactory;
import ru.gustos.trading.book.Sheet;

import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class GustosIndicator extends Indicator implements IIndicatorWithProperties{
    public static int Id;
    BotLogic botLogic;

    public GustosIndicator(IndicatorInitData data) {
        super(data);
        Id = data.id;
    }


    @Override
    public Map<String,String> getMarks(int ind) {
        final BotAdvice ose = botLogic.getBotAdvice(ind, null, true);
        return ose.getDecisionArgs();
    }

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {

        if (botLogic==null) {
            botLogic = LogicFactory.Companion.getLogic(data.logic,
                    Instrument.Companion.getBTC_USDT(),
                    BarInterval.ONE_MIN,
                    sheet.moments.stream()
                            .map(m -> new XExtBar(m.bar))
                            .collect(Collectors.toList()));

            botLogic.loadState(data.state);
        }

        for (int i = from; i < to; i++) {

            final BotAdvice ose = botLogic.getBotAdvice(i, null, true);
            values[0][i] = ose.getDecision() == Decision.BUY ? Indicator.YES : Indicator.NO;

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

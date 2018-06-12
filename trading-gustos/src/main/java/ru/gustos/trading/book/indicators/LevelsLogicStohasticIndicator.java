package ru.gustos.trading.book.indicators;

import ru.efreet.trading.Decision;
import ru.efreet.trading.bars.XExtBar;
import ru.gustos.trading.LevelsBotLogic;
import ru.gustos.trading.book.Sheet;

public class LevelsLogicStohasticIndicator extends NumberIndicator{

    public LevelsLogicStohasticIndicator(IndicatorInitData data) {
        super(data);
    }


    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        GustosIndicator indicator = (GustosIndicator)sheet.getLib().get(data.ind);
        LevelsBotLogic logic = (LevelsBotLogic) indicator.botLogic;
        for (int i = 0;i<sheet.size();i++) {
            XExtBar bar = logic.getBar(i);
            Decision stohastic = bar.getStohastic();
            values[0][i] = stohastic==Decision.NONE?0:(stohastic==Decision.BUY?1:-1);
        }
    }


}


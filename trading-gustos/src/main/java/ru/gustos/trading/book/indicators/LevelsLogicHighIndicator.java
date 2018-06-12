package ru.gustos.trading.book.indicators;

import ru.efreet.trading.Decision;
import ru.efreet.trading.bars.XExtBar;
import ru.gustos.trading.LevelsBotLogic;
import ru.gustos.trading.book.Sheet;

public class LevelsLogicHighIndicator extends Indicator{

    public LevelsLogicHighIndicator(IndicatorInitData data) {
        super(data);
    }

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        GustosIndicator indicator = (GustosIndicator)sheet.getLib().get(data.ind);
        LevelsBotLogic logic = (LevelsBotLogic) indicator.botLogic;
        for (int i = 0;i<sheet.size();i++) {
            XExtBar bar = logic.getBar(i);
            values[0][i] = bar.getSd2()>0?1:0;
        }
    }

}

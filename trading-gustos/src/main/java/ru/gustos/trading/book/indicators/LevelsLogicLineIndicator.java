package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XExtBar;
import ru.gustos.trading.LevelsBotLogic;
import ru.gustos.trading.book.Sheet;

public class LevelsLogicLineIndicator extends NumberIndicator{

    public LevelsLogicLineIndicator(IndicatorInitData data) {
        super(data);
    }

    @Override
    public IndicatorVisualType getVisualType() {
        return IndicatorVisualType.PRICELINE;
    }

    @Override
    public int getNumberOfLines() {
        return 2;
    }

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        GustosIndicator indicator = (GustosIndicator)sheet.getLib().get(data.ind);
        LevelsBotLogic logic = (LevelsBotLogic) indicator.botLogic;
        for (int i = 0;i<sheet.size();i++) {
            XExtBar bar = logic.getBar(i);
            values[0][i] = bar.getSma();
            values[1][i] = bar.getSma()+bar.getSd();//*logic.getParams().getDiviation()*0.1;
        }
    }

}


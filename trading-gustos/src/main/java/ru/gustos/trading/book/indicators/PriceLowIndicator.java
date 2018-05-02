package ru.gustos.trading.book.indicators;

import ru.efreet.trading.logic.impl.sd3.Sd3Logic;
import ru.efreet.trading.logic.impl.sd5.Sd5Logic;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;

public class PriceLowIndicator extends BaseIndicator {
    int deviation;
    boolean howlong;

    public PriceLowIndicator(IndicatorInitData data){
        super(data);
        deviation = data.t1;
        howlong = data.b1;
    }

    @Override
    public String getName() {
        return "PriceLow_"+howlong+"_"+deviation;
    }

    @Override
    public IndicatorType getType() {
        return howlong?IndicatorType.NUMBER:IndicatorType.YESNO;
    }

    int l = 0;
    int lastTo = -1;

    @Override
    public void calcValues(Sheet sheet, double[] values, int from, int to) {
        EfreetIndicator indicator = (EfreetIndicator)sheet.getLib().get(EfreetIndicator.Id);
        Sd5Logic botLogic = (Sd5Logic)indicator.botLogic;
        if (from!=lastTo){
            from  = 0;
            l = 0;
        }
        for (int i = from;i<to;i++) {
            boolean res = botLogic.priceLow(i,deviation);
            if (res)
                l++;
            else
                l = 0;
            values[i] = howlong?l:(res?IIndicator.YES:IIndicator.NO);
        }
        lastTo = to;
    }

    @Override
    public Color getColorMax() {
        return CandlesPane.GREEN;
    }

    @Override
    public Color getColorMin() {
        return Color.RED;
    }
}

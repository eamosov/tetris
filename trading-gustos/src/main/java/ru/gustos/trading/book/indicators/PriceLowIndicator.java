package ru.gustos.trading.book.indicators;

import ru.efreet.trading.logic.impl.sd3.Sd3Logic;
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

    @Override
    public void calcValues(Sheet sheet, double[] values) {
        EfreetIndicator indicator = (EfreetIndicator)sheet.getLib().get(EfreetIndicator.Id);
        Sd3Logic botLogic = (Sd3Logic)indicator.botLogic;
        int l = 0;
        for (int i = 0;i<sheet.moments.size();i++) {
            boolean res = botLogic.priceLow(i,deviation);
            if (res)
                l++;
            else
                l = 0;
            values[i] = howlong?l:(res?IIndicator.YES:IIndicator.NO);

        }
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

package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;


public class PriceChangeIndicator extends NumberIndicator {

    IndicatorPeriod period;

    public PriceChangeIndicator(IndicatorInitData data){
        super(data);
        period =  IndicatorPeriod.values()[data.period];
    }

    @Override
    public String getName() {
        return "PriceChange_"+period.name();
    }


    @Override
    public void calcValues(Sheet sheet, double[] values, int from, int to) {
        int bars = IndicatorUtils.bars(period,sheet);
        for (int i = from;i<to;i++)
            values[i] =  (sheet.moments.get(i).bar.getClosePrice()/sheet.moments.get(Math.max(0,i-bars)).bar.getClosePrice()-1)*10;
    }
}


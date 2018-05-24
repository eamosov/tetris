package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;

public class RelativePriceIndicator extends NumberIndicator {
    IndicatorPeriod period;

    public RelativePriceIndicator(IndicatorInitData data){
        super(data);
        period =  IndicatorPeriod.values()[data.period];
    }


    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        int bars = IndicatorUtils.bars(period,sheet);
//        if (bars<20) {
            for (int i = from; i < to; i++){
                XBar bar = sheet.bar(i);
                double min = bar.getClosePrice();
                double max = bar.getClosePrice();
                for (int j = Math.max(0,i-bars);j<i;j++) {
                    XBar b = sheet.moments.get(j).bar;
                    if (min>b.getMinPrice()) min = b.getMinPrice();
                    if (max<b.getMaxPrice()) max = b.getMaxPrice();
                }
                if (max-min<0.001)
                    values[0][i] = 0.5;
                else
                    values[0][i] = (bar.getClosePrice()-min)/(max-min);
            }
//        }
    }

    @Override
    public double getUpperBound() {
        return 1.0;
    }

    @Override
    public double getLowerBound() {
        return 0;
    }
}


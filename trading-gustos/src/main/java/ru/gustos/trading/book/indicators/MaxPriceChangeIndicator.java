package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;

public class MaxPriceChangeIndicator extends NumberIndicator {
    IndicatorPeriod period;

    public MaxPriceChangeIndicator(IndicatorInitData data){
        super(data);
        period =  IndicatorPeriod.values()[data.period];
    }


    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        int bars = IndicatorUtils.bars(period,sheet);
        if (data.positive) {
            for (int i = Math.max(bars,from); i < to; i++) {
                double cur = sheet.bar(i).getClosePrice();
                double min = cur;
                for (int j = 0;j<=bars;j++){
                    double v = sheet.moments.get(i-bars+j).bar.getMinPrice();
                    if (v<min) min = v;
                }
                values[0][i] = (cur/min-1)*10;

            }
        } else {
            for (int i = Math.max(bars,from); i < to; i++) {
                double cur = sheet.bar(i).getClosePrice();
                double max = cur;
                for (int j = 0;j<=bars;j++){
                    double v = sheet.moments.get(i-bars+j).bar.getMaxPrice();
                    if (v>max) max = v;
                }
                values[0][i] = (max/cur-1)*10;

            }
        }
    }

}



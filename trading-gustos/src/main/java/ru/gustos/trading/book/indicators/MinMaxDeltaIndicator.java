package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;

public class MinMaxDeltaIndicator extends NumberIndicator {

    int t1;

    public MinMaxDeltaIndicator(IndicatorInitData data){
        super(data);
        t1 = data.t1;
    }

    @Override
    public String getName() {
        return "minmaxdelta_"+t1;
    }


    @Override
    public void calcValues(Sheet sheet, double[] values, int from, int to) {
        for (int i = Math.max(t1,from);i<to;i++) {
            XBar bar = sheet.bar(i-t1);
            values[i] =  (bar.getMaxPrice()-bar.getMinPrice())/bar.middlePrice()*100;
        }
    }

}


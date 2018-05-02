package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;

public class DeltaIndicator extends NumberIndicator {

    int t1;

    public DeltaIndicator(IndicatorInitData data){
        super(data);
        t1 = data.t1;
    }

    @Override
    public String getName() {
        return "delta_"+t1;
    }


    @Override
    public void calcValues(Sheet sheet, double[] values, int from, int to) {
        for (int i = Math.max(from,t1);i<to;i++) {
            XBar bar = sheet.moments.get(i-t1).bar;
            values[i] =  (bar.getClosePrice()-bar.getOpenPrice())/bar.middlePrice()*100;
        }
    }

}


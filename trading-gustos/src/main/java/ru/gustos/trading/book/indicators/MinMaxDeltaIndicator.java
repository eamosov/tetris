package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;

public class MinMaxDeltaIndicator extends NumberIndicator {


    public MinMaxDeltaIndicator(IndicatorInitData data){
        super(data);
    }


    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        for (int i = Math.max(data.t1,from);i<to;i++) {
            XBar bar = sheet.bar(i-data.t1);
            values[0][i] =  (bar.getMaxPrice()-bar.getMinPrice())/bar.middlePrice()*100;
        }
    }

}


package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;

import java.awt.*;

public class MinMaxToVolumeIndicator extends NumberIndicator {

    int t1;

    public MinMaxToVolumeIndicator(IndicatorInitData data){
        super(data);
        t1 = data.t1;
    }

    @Override
    public String getName() {
        return "minmaxtovolume_"+t1;
    }


    @Override
    public void calcValues(Sheet sheet, double[] values) {
        for (int i = t1;i<values.length;i++) {
            XBar bar = sheet.moments.get(i-t1).bar;
            values[i] =  (bar.getMaxPrice()-bar.getMinPrice())/bar.middlePrice()*1000/bar.getVolume();
        }
    }

}


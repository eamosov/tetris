package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;

public class DeltaToVolumeIndicator extends NumberIndicator {

    int t1;

    public DeltaToVolumeIndicator(IndicatorInitData data){
        super(data);
        t1 = data.t1;
    }

    @Override
    public String getName() {
        return "deltatovolume_"+t1;
    }


    @Override
    public void calcValues(Sheet sheet, double[] values) {
        for (int i = t1;i<values.length;i++) {
            XBar bar = sheet.moments.get(i-t1).bar;
            values[i] =  Math.abs(bar.getOpenPrice()-bar.getClosePrice())/bar.middlePrice()*1000/bar.getVolume();
        }
    }

}


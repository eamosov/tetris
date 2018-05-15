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
    public void calcValues(Sheet sheet, double[] values, int from, int to) {
        for (int i = Math.max(from,t1);i<to;i++) {
            XBar bar = sheet.bar(i-t1);
            values[i] =  Math.abs(bar.getOpenPrice()-bar.getClosePrice())/bar.middlePrice()*1000/bar.getVolume();
        }
    }

}


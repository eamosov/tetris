package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;

public class DeltaFromNowIndicator extends NumberIndicator {

    int t1;

    public DeltaFromNowIndicator(IndicatorInitData data){
        super(data);
        t1 = data.t1;
    }

    @Override
    public String getName() {
        return "deltafromnow_"+t1;
    }


    @Override
    public void calcValues(Sheet sheet, double[] values) {
        for (int i = t1;i<values.length;i++) {
            XBar bar = sheet.moments.get(i-t1).bar;
            XBar barn = sheet.moments.get(i).bar;
            values[i] =  (barn.getClosePrice()-bar.getMinPrice())/bar.middlePrice()*100;
        }
    }

}


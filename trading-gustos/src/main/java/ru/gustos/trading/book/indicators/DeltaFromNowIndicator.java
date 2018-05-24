package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;

public class DeltaFromNowIndicator extends NumberIndicator {


    public DeltaFromNowIndicator(IndicatorInitData data){
        super(data);
    }


    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        for (int i = Math.max(from,data.t1);i<to;i++) {
            XBar bar = sheet.bar(i-data.t1);
            XBar barn = sheet.bar(i);
            values[0][i] =  (barn.getClosePrice()-bar.getClosePrice())/bar.middlePrice()*100;
        }
    }

}


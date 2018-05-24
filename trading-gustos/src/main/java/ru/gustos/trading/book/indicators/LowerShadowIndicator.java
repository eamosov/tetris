package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;

public class LowerShadowIndicator extends NumberIndicator {


    public LowerShadowIndicator(IndicatorInitData data){
        super(data);
    }


    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        for (int i = Math.max(from,data.t1);i<to;i++) {
            XBar bar = sheet.bar(i-data.t1);
            values[0][i] =  (Math.min(bar.getOpenPrice(),bar.getClosePrice())-bar.getMinPrice())/bar.middlePrice()*100;
        }
    }

}

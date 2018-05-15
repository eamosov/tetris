package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;

public class LowerShadowIndicator extends NumberIndicator {

    int t1;

    public LowerShadowIndicator(IndicatorInitData data){
        super(data);
        t1 = data.t1;
    }

    @Override
    public String getName() {
        return "lower_"+t1;
    }


    @Override
    public void calcValues(Sheet sheet, double[] values, int from, int to) {
        for (int i = Math.max(from,t1);i<to;i++) {
            XBar bar = sheet.bar(i-t1);
            values[i] =  (Math.min(bar.getOpenPrice(),bar.getClosePrice())-bar.getMinPrice())/bar.middlePrice()*100;
        }
    }

}

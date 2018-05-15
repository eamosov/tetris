package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;

public class UpperShadowIndicator extends NumberIndicator {

    int t1;

    public UpperShadowIndicator(IndicatorInitData data){
        super(data);
        t1 = data.t1;
    }

    @Override
    public String getName() {
        return "upper_"+t1;
    }


    @Override
    public void calcValues(Sheet sheet, double[] values, int from, int to) {
        for (int i = Math.max(from,t1);i<to;i++) {
            XBar bar = sheet.bar(i-t1);
            values[i] =  (bar.getMaxPrice()-Math.max(bar.getOpenPrice(),bar.getClosePrice()))/bar.middlePrice()*100;
        }
    }

}


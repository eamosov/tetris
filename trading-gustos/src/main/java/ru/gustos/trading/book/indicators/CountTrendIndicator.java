package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;

public class CountTrendIndicator extends NumberIndicator {
    int t1;
    boolean positive;


    public CountTrendIndicator(IndicatorInitData data){
        super(data);
        t1 = data.t1;
        positive = data.positive;
    }

    @Override
    public String getName() {
        return "counttrend_"+t1+"_"+(positive?"pos":"neg");
    }


    @Override
    public void calcValues(Sheet sheet, double[] values) {
        for (int i = t1+20;i<values.length;i++) {
            int cc = 0;
            while (true){
                XBar bar = sheet.moments.get(i-t1-cc).bar;
                if (positive && bar.isBearish()) break;
                if (!positive && bar.isBullish()) break;
                cc++;
            }
            values[i] = cc;
        }
    }

}


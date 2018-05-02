package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;

public class CountComovesIndicator extends NumberIndicator {
    int t1;
    boolean positive;


    public CountComovesIndicator(IndicatorInitData data){
        super(data);
        t1 = data.t1;
        positive = data.positive;
    }

    @Override
    public String getName() {
        return "countcomoves_"+t1+"_"+(positive?"pos":"neg");
    }


    @Override
    public void calcValues(Sheet sheet, double[] values, int from, int to) {
        for (int i = Math.max(from,t1+10);i<to;i++) {
            int cc = 0;
            for (int j = 0;j<10;j++){
                XBar bar = sheet.moments.get(i-t1-j).bar;
                if (positive && bar.isBullish()) cc++;
                if (!positive && bar.isBearish()) cc++;
            }
            values[i] = cc;
        }
    }

}

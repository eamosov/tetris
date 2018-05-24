package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;

public class CountComovesIndicator extends NumberIndicator {


    public CountComovesIndicator(IndicatorInitData data){
        super(data);
    }

    @Override
    public double getLowerBound() {
        return 0;
    }

    @Override
    public double getUpperBound() {
        return 10;
    }

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        for (int i = Math.max(from,data.t1+10);i<to;i++) {
            int cc = 0;
            for (int j = 0;j<10;j++){
                XBar bar = sheet.moments.get(i-data.t1-j).bar;
                if (data.positive && bar.isBullish()) cc++;
                if (!data.positive && bar.isBearish()) cc++;
            }
            values[0][i] = cc;
        }
    }

}

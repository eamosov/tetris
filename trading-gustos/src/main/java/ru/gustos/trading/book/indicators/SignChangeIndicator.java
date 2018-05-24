package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

public class SignChangeIndicator extends Indicator {

    public SignChangeIndicator(IndicatorInitData data){
        super(data);
    }

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        int bars = data.t1*5;
        double[] vv = sheet.getData().get(data.ind);
        for (int i = Math.max(from,bars);i<to;i++){
            double v = vv[i];
            if (data.positive)
                values[0][i] = (v > 0 && vv[i-bars]<0)? Indicator.YES: Indicator.NO;
            else
                values[0][i] = (v < 0 && vv[i-bars]>0)? Indicator.YES: Indicator.NO;
        }
    }
}

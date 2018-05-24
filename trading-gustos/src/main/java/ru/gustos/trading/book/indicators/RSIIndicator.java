package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBaseBar;
import ru.gustos.trading.book.Sheet;

import java.awt.*;
import java.util.Arrays;

public class RSIIndicator extends NumberIndicator {

    public RSIIndicator(IndicatorInitData data){
        super(data);
    }

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        double[] v1 = new double[values.length];
        double[] v2 = new double[v1.length];
        XBaseBar prev = (XBaseBar)sheet.moments.get(0).bar;
        for (int i = Math.max(1,from);i<to;i++){
            XBaseBar b = (XBaseBar)sheet.bar(i);
            double d = b.getClosePrice() - prev.getClosePrice();
            if (d>0)
                v1[i] = d;
            if (d<0)
                v2[i] = -d;

            prev = b;
        }
        v1 = VecUtils.ema(v1,data.t1);
        v2 = VecUtils.ema(v2,data.t1);
        for (int i = 0;i<v1.length;i++){
            double v = v1[i] + v2[i];
            values[0][i] = v==0?100:100*v1[i]/ v-50;
        }
    }
}


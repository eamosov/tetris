package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBaseBar;
import ru.gustos.trading.book.Sheet;

import java.awt.*;
import java.util.Arrays;

public class RSIIndicator extends NumberIndicator {

    int t1;
    int period;

    public RSIIndicator(IndicatorInitData data){
        super(data);
        t1 = data.t1;
        period = data.period;
    }

    @Override
    public String getName() {
        return "rsi_"+period+"_"+t1;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {
        double[] v1 = new double[(values.length+period-1)/period];
        double[] v2 = new double[v1.length];
        XBaseBar prev = sheet.getSumBar(0, period);
        for (int i = period;i<values.length;i+=period){
            XBaseBar b = sheet.getSumBar(i, period);
            double d = b.getClosePrice() - prev.getClosePrice();
            if (d>0)
                v1[i/period] = d;
            if (d<0)
                v2[i/period] = -d;

            prev = b;
        }
        v1 = VecUtils.ema(v1,t1);
        v2 = VecUtils.ema(v2,t1);
        for (int i = 0;i<v1.length;i++){
            double v = v1[i] + v2[i];
            v1[i] = v==0?100:100*v1[i]/ v-50;
        }
        System.arraycopy(VecUtils.resize(v1,values.length),0,values,0,values.length);

    }
}


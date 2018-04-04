package ru.efreet.trading.book.indicators;

import ru.efreet.trading.book.Sheet;

import java.awt.*;

public class DemaIndicator implements IIndicator {
    public static final int Id = 5;

    @Override
    public int getId() {
        return Id;
    }

    @Override
    public String getName() {
        return "dema";
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.NUMBER;
    }

    @Override
    public Color getColorMax() {
        return Color.green;
    }

    @Override
    public Color getColorMin() {
        return Color.red;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {
        double k1 = 2.0/(26+1);
        double k2 = 2.0/(12+1);
        double ks = 2.0/(9+1);
        double[] ema1 = new double[values.length];
        double[] ema2 = new double[values.length];
        double[] emas = new double[values.length];
        double[] dema1 = new double[values.length];
        double[] dema2 = new double[values.length];
        double[] demas = new double[values.length];
        ema2[0] = dema1[0] = dema2[0] = ema1[0] = 0;
        for (int i = 1;i<values.length;i++){
            double p = sheet.moments.get(i).bar.getClosePrice();
            ema1[i] = (p-ema1[i-1])*k1+ema1[i-1];
            ema2[i] = (p-ema2[i-1])*k2+ema2[i-1];

            dema1[i] = (ema1[i]- dema1[i-1])*k1+dema1[i-1];
            dema2[i] = (ema2[i]- dema2[i-1])*k2+dema2[i-1];

            double d1 = ema1[i]*2-dema1[i];
            double d2 = ema2[i]*2-dema2[i];
            double macd = d2 - d1;
            emas[i] = (macd - emas[i-1])*ks + emas[i-1];
            demas[i] = (emas[i] - demas[i-1])*ks+demas[i-1];
            double ds = emas[i]*2 - demas[i];

            values[i] = macd - ds;

//            double macd = ema2[i]-ema1[i];
//            emas[i] = (macd-emas[i-1])*ks+emas[i-1];
//            values[i] = macd - emas[i];

        }

    }
}

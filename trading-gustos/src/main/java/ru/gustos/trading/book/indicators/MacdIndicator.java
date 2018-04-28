package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;

public class MacdIndicator  extends BaseIndicator {
    int t1,t2,t3;
    boolean yesno;

    public MacdIndicator(IndicatorInitData data){
        super(data);
        t1 = data.t1;
        t2 = data.t2;
        t3 = data.t3;
        yesno = data.b1;
    }

    @Override
    public IndicatorType getType() {
        return yesno?IndicatorType.YESNO:IndicatorType.NUMBER;
    }

    @Override
    public Color getColorMax() {
        return CandlesPane.GREEN;
    }

    @Override
    public Color getColorMin() {        return CandlesPane.RED;    }

    @Override
    public String getName() {
        return "macd_"+t1+"_"+yesno;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {
        double k1 = 2.0/(t1+1);
        double k2 = 2.0/(t2+1);
        double ks = 2.0/(t3+1);
        double[] ema1 = new double[values.length];
        double[] ema2 = new double[values.length];
        double[] emas = new double[values.length];
        ema2[0] = emas[0] = ema1[0] = sheet.moments.get(0).bar.getClosePrice();
        for (int i = 1;i<values.length;i++){
            double p = sheet.moments.get(i).bar.getClosePrice();
            ema1[i] = (p-ema1[i-1])*k1+ema1[i-1];
            ema2[i] = (p-ema2[i-1])*k2+ema2[i-1];
            double macd = ema2[i]-ema1[i];
            emas[i] = (macd-emas[i-1])*ks+emas[i-1];
            double vv = macd - emas[i];
            values[i] = yesno?(vv>0?IIndicator.YES:IIndicator.NO):vv;
        }

    }
}


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


    private int lastTo = -1;
    private double ema1p, ema2p, emasp;

    @Override
    public void calcValues(Sheet sheet, double[] values, int from, int to) {
        double k1 = 2.0/(t1+1);
        double k2 = 2.0/(t2+1);
        double ks = 2.0/(t3+1);
        double ema1;
        double ema2;
        double emas;
        if (from!=lastTo) {
            from = 1;
            ema1p = sheet.moments.get(0).bar.getClosePrice();
            ema2p = ema1p;
            emasp = 0;
            values[0] = yesno?IIndicator.YES:ema1p;
        }
        for (int i = from;i<to;i++){
            double p = sheet.moments.get(i).bar.getClosePrice();
            ema1 = (p-ema1p)*k1+ema1p;
            ema2 = (p-ema2p)*k2+ema2p;
            double macd = ema2-ema1;
            emas = (macd-emasp)*ks+emasp;
            double vv = macd - emas;
            values[i] = yesno?(vv>0?IIndicator.YES:IIndicator.NO):vv;

            ema1p = ema1;
            ema2p = ema2;
            emasp = emas;
        }
        lastTo = to;

    }
}


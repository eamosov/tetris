package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

public class DemaIndicator extends Indicator {

    boolean yesno;

    public DemaIndicator(IndicatorInitData data){
        super(data);
        yesno = data.b1;
    }

    public IndicatorResultType getResultType() {
        return yesno? IndicatorResultType.YESNO: IndicatorResultType.NUMBER;
    }

    private int lastTo = -1;
    private double ema1p, ema2p, emasp, dema1p, dema2p, demasp;

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        double k1 = 2.0/(data.t1+1);
        double k2 = 2.0/(data.t2+1);
        double ks = 2.0/(data.t3+1);
        double ema1;
        double ema2;
        double emas;
        double dema1;
        double dema2;
        double demas;
        if (from!=lastTo) {
            from = 1;
            ema1p = sheet.moments.get(0).bar.getClosePrice();
            ema2p = ema1p;
            emasp = 0;
            dema1p = ema1p;
            dema2p = ema1p;
            demasp = 0;
            values[0][0] = yesno? Indicator.YES:dema1p;
        }
        for (int i = from;i<to;i++){

            double p = sheet.bar(i).getClosePrice();
            ema1 = (p-ema1p)*k1+ema1p;
            ema2 = (p-ema2p)*k2+ema2p;

            dema1 = (ema1- dema1p)*k1+dema1p;
            dema2 = (ema2- dema2p)*k2+dema2p;

            double d1 = ema1*2-dema1;
            double d2 = ema2*2-dema2;
            double macd = d2 - d1;
            emas = (macd - emasp)*ks + emasp;
            demas = (emas - demasp)*ks+demasp;
            double ds = emas*2 - demas;

            double vv = macd - ds;
            values[0][i] = yesno?(vv>0? Indicator.YES: Indicator.NO):vv;

            ema1p = ema1;
            ema2p = ema2;
            emasp = emas;
            dema1p = dema1;
            dema2p = dema2;
            demasp = demas;

        }
        lastTo = to;
    }
}


package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

public class MacdIndicator  extends Indicator {
    boolean yesno;

    public MacdIndicator(IndicatorInitData data){
        super(data);
        yesno = data.b1;
    }

    @Override
    public IndicatorResultType getResultType() {
        return yesno? IndicatorResultType.YESNO: IndicatorResultType.NUMBER;
    }


    private int lastTo = -1;
    private double ema1p, ema2p, emasp;

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        double k1 = 2.0/(data.t1+1);
        double k2 = 2.0/(data.t2+1);
        double ks = 2.0/(data.t3+1);
        double ema1;
        double ema2;
        double emas;
        if (from!=lastTo) {
            from = 1;
            ema1p = sheet.moments.get(0).bar.getClosePrice();
            ema2p = ema1p;
            emasp = 0;
            values[0][0] = yesno? Indicator.YES:ema1p;
        }
        for (int i = from;i<to;i++){
            double p = sheet.bar(i).getClosePrice();
            ema1 = (p-ema1p)*k1+ema1p;
            ema2 = (p-ema2p)*k2+ema2p;
            double macd = ema2-ema1;
            emas = (macd-emasp)*ks+emasp;
            double vv = macd - emas;
            values[0][i] = yesno?(vv>0? Indicator.YES: Indicator.NO):vv;

            ema1p = ema1;
            ema2p = ema2;
            emasp = emas;
        }
        lastTo = to;

    }
}


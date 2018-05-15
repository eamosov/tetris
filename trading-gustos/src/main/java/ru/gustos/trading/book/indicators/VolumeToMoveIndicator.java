package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;

public class VolumeToMoveIndicator extends NumberIndicator {

    private final boolean positive;
    int t1;


    public VolumeToMoveIndicator(IndicatorInitData data){
        super(data);
        t1 = data.t1;
        positive = data.positive;
    }

    @Override
    public String getName() {
        return "volumetomove_"+positive+"_"+t1;
    }

    private int lastTo = -1;
    private double ema1p;
    private double ema2p;

    @Override
    public void calcValues(Sheet sheet, double[] values, int from, int to) {
        double k = 2.0/(t1+1);

        double ema1 = 0;
        double ema2 = 0;
        if (from!=lastTo) {
            from = 1;
            ema1p = 0;
            ema2p = 0;
            values[0] = 0;
        }

        for (int i = from;i<to;i++) {
            XBar bar = sheet.bar(i);
            double p = bar.delta()*bar.delta()/Math.max(0.1,bar.getVolumeBase());
            if (bar.delta()>0){
                ema1 = (p-ema1p)*k+ema1p;
                ema1p = ema1;
            } else if (bar.delta()<0) {
                p = Math.abs(p);
                ema2 = (p-ema2p)*k+ema2p;
                ema2p = ema2;
            }

            values[i] = ema2==0?0:ema1/ema2-1;

        }
        lastTo = to;
    }

    @Override
    public boolean fromZero() {
        return false;
    }

    @Override
    public Color getColorMax() {
        return CandlesPane.GREEN;
    }
    @Override
    public Color getColorMin() {
        return CandlesPane.RED;
    }
//    @Override
//    public Color getColorMax() {
//        return positive? CandlesPane.GREEN: CandlesPane.RED;
//    }
//    @Override
//    public Color getColorMin() {
//        return Color.lightGray;
//    }
}

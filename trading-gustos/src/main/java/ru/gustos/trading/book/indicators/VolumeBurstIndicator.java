package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;

public class VolumeBurstIndicator extends BaseIndicator {
    int t1,t2;
    boolean volumeToPriceDelta;
    public VolumeBurstIndicator(IndicatorInitData data){
        super(data);
        t1 = data.t1;
        t2 = data.t2;
        volumeToPriceDelta = data.b1;
    }

    @Override
    public String getName() {
        return "VolumeBurst_"+t1+"_"+t2+(volumeToPriceDelta ?"_toDelta":"");
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.NUMBER;
    }

    private int lastTo = -1;
    private double ema1p, ema2p;
    @Override
    public void calcValues(Sheet sheet, double[] values, int from, int to) {
        double[] vols = volumeToPriceDelta ?sheet.moments.stream().mapToDouble(m -> m.bar.getVolumeBase()/Math.max(10,Math.abs(m.bar.deltaMaxMin()))).toArray():
                sheet.moments.stream().mapToDouble(m -> m.bar.getVolume()).toArray();
        double k1 = 2.0/(t1+1);
        double k2 = 2.0/(t2+1);

        double ema1;
        double ema2;
        if (from!=lastTo) {
            from = 1;
            ema1p = vols[0];
            ema2p = ema1p;
            values[0] = ema1p;
        }

        for (int i = from;i<to;i++) {
            double p = vols[i];
            ema1 = (p-ema1p)*k1+ema1p;
            ema2 = (p-ema2p)*k2+ema2p;
            values[i] = ema1/ema2-1;
            ema1p = ema1;
            ema2p = ema2;
        }
        lastTo = to;
    }

    @Override
    public Color getColorMax() {
        return CandlesPane.GREEN;
    }

    @Override
    public Color getColorMin() {
        return Color.RED;
    }
}


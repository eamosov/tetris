package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.logic.impl.sd3.Sd3Logic;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;

public class VolumeBurstIndicator extends BaseIndicator {
    int t1,t2;
    public VolumeBurstIndicator(IndicatorInitData data){
        super(data);
        t1 = data.t1;
        t2 = data.t2;
    }

    @Override
    public String getName() {
        return "VolumeBurst_"+t1+"_"+t2;
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.NUMBER;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {
        double[] vols = sheet.moments.stream().mapToDouble(m -> m.bar.getVolume()).toArray();
        double[] ema1 = VecUtils.ema(vols, t1);
        double[] ema2 = VecUtils.ema(vols, t2);
        for (int i = 0;i<sheet.moments.size();i++) {
            values[i] = ema1[i]/ema2[i]-1;
        }
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


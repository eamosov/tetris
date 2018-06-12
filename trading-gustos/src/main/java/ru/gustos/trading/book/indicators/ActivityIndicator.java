package ru.gustos.trading.book.indicators;

import kotlin.Pair;
import ru.gustos.trading.book.Sheet;

public class ActivityIndicator extends NumberIndicator{

    public ActivityIndicator(IndicatorInitData data) {
        super(data);
    }


    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        Pair<double[], double[]> pair = VecUtils.emaAndDisp(sheet.moments.stream().mapToDouble(m -> m.bar.getClosePrice()).toArray(), data.t1);
        double[] vols = VecUtils.ema(sheet.moments.stream().mapToDouble(m -> m.bar.getVolume()).toArray(), data.t1);
        for (int i = 0;i<sheet.size();i++) {
            values[0][i] = (1+pair.getSecond()[i]*vols[i]);
        }
    }

    @Override
    public double getLowerBound() {
        return 0;
    }
}

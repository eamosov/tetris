package ru.gustos.trading.book.indicators;

import kotlin.Pair;
import ru.gustos.trading.GustosBotLogic2;
import ru.gustos.trading.book.Sheet;

import java.awt.*;

import static ru.gustos.trading.book.indicators.VolumeIndicator.COLOR;
import static ru.gustos.trading.book.indicators.VolumeIndicator.COLORMIN;

public class MultiAverageIndicator extends BaseIndicator{

    boolean positive;

    public MultiAverageIndicator(IndicatorInitData data){
        super(data);
        positive = data.positive;
    }

    @Override
    public String getName() {
        return "multiav";
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.NUMBER;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values, int from, int to) {
//        GustosBotLogic2 logic = (GustosBotLogic2) (GustosBotLogic2) ((GustosIndicator) sheet.getLib().get(400)).botLogic;
        double[] v = sheet.moments.stream().mapToDouble(m -> m.bar.getClosePrice()).toArray();
        double[] vols = sheet.moments.stream().mapToDouble(m -> m.bar.getVolume()).toArray();
        Pair<double[], double[]>[] gars = new Pair[5];
        for (int i = 0;i<gars.length;i++) {
            int window = 10*(1<<(i*2));
            gars[i] = GustosAverageRecurrent.calc(v, window, vols, window * 4);
        }
        for (int j = 0;j<gars.length;j++) {
            double[] a = gars[j].getFirst();
            double[] d = gars[j].getSecond();
            for (int i = 1; i < values.length; i++) {
                    if (v[i] > a[i] + d[i]*1)
                        values[i] += 1;
                    if (v[i] < a[i-1] - d[i-1]*1)
                        values[i] -= 1;
            }
        }

    }

    @Override
    public Color getColorMax() {
        return Color.green;
    }

    public Color getColorMin() {        return Color.red;    }

}

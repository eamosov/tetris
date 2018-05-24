package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

import java.awt.*;

public class RelativeVolumeIndicator extends NumberIndicator {
    IndicatorPeriod period;

    public RelativeVolumeIndicator(IndicatorInitData data){
        super(data);
        period =  IndicatorPeriod.values()[data.period];
    }


    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        int bars = IndicatorUtils.bars(period,sheet);
        double sum = 0;
        for (int i = from; i < to; i++){
            double vol = sheet.bar(i).getVolume();
            double avg;
            sum+=vol;
            if (i>=bars){
                sum-=sheet.moments.get(i-bars).bar.getVolume();
                avg = sum/bars;
            } else {
                avg = sum/(i+1);
            }
            if (avg<0.0001)
                values[0][i] = 0;
            else
                values[0][i] =vol/avg;
        }
    }

    @Override
    public ColorScheme getColors() {
        return ColorScheme.WHITEBLUE;
    }

    @Override
    public double getLowerBound() {
        return 0;
    }
}


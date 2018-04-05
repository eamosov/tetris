package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

import java.awt.*;

public class RelativeVolumeIndicator implements IIndicator{
    public static final int Id = 30;

    IndicatorPeriod period;

    public RelativeVolumeIndicator(IndicatorPeriod period){
        this.period = period;
    }

    @Override
    public int getId() {
        return Id + period.ordinal();
    }

    @Override
    public String getName() {
        return "RelativeVolume_"+period.name();
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.NUMBER;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {
        int bars = IndicatorUtils.bars(period,sheet);
        double sum = 0;
        for (int i = 0; i < values.length; i++){
            double vol = sheet.moments.get(i).bar.getVolume();
            double avg;
            sum+=vol;
            if (i>=bars){
                sum-=sheet.moments.get(i-bars).bar.getVolume();
                avg = sum/bars;
            } else {
                avg = sum/(i+1);
            }
            if (avg<0.0001)
                values[i] = 0;
            else
                values[i] =vol/avg;
        }
    }

    @Override
    public Color getColorMax() {
        return Color.blue;
    }

    public Color getColorMin() {        return Color.white;    }

    @Override
    public boolean fromZero() {
        return true;
    }
}


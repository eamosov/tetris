package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

import java.awt.*;

public class MinMaxToVolumeSIndicator extends BaseIndicator{


    public MinMaxToVolumeSIndicator(IndicatorInitData data){
        super(data);
    }

    @Override
    public String getName() {
        return "MinMaxToVolume";
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.NUMBER;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {
        for (int i = 0;i<values.length;i++)
            values[i] =  Math.abs(sheet.moments.get(i).bar.deltaMaxMin())/Math.max(0.1,sheet.moments.get(i).bar.getVolume());
    }

    @Override
    public Color getColorMax() {
        return VolumeIndicator.COLOR;
    }

    public Color getColorMin() {        return VolumeIndicator.COLORMIN;    }

    @Override
    public boolean fromZero() {
        return true;
    }
}

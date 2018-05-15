package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

import java.awt.*;

public class VolumeBaseIndicator extends BaseIndicator{


    public VolumeBaseIndicator(IndicatorInitData data){
        super(data);
    }

    @Override
    public String getName() {
        return "Volume_base";
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.NUMBER;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values, int from, int to) {
        for (int i = from;i<to;i++)
            values[i] =  sheet.bar(i).getVolumeBase();
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


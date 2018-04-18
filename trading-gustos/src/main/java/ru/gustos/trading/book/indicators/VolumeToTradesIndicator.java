package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

import java.awt.*;

public class VolumeToTradesIndicator extends BaseIndicator{


    public VolumeToTradesIndicator(IndicatorInitData data){
        super(data);
    }

    @Override
    public String getName() {
        return "Volume_to_trades";
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.NUMBER;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {
        for (int i = 0;i<values.length;i++)
            values[i] =  sheet.moments.get(i).bar.getVolume()/sheet.moments.get(i).bar.getTrades();
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


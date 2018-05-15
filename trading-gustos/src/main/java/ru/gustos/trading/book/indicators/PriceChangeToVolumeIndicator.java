package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

import java.awt.*;

public class PriceChangeToVolumeIndicator extends BaseIndicator{


    public PriceChangeToVolumeIndicator(IndicatorInitData data){
        super(data);
    }

    @Override
    public String getName() {
        return "PriceChangeToVolume";
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.NUMBER;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values, int from, int to) {
        for (int i = from;i<to;i++)
            values[i] =  (sheet.bar(i).getVolumeQuote()/sheet.bar(i).middlePrice())/Math.max(10,Math.abs(sheet.bar(i).deltaMaxMin()));
//            values[i] =  Math.abs(sheet.bar(i).delta())/Math.max(0.1,sheet.bar(i).getVolume());
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


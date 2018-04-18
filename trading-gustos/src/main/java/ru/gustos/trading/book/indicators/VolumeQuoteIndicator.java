package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

import java.awt.*;

public class VolumeQuoteIndicator extends BaseIndicator{



    public VolumeQuoteIndicator(IndicatorInitData data){
        super(data);
    }

    @Override
    public String getName() {
        return "Volume_quote";
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.NUMBER;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {
        for (int i = 0;i<values.length;i++)
            values[i] =  sheet.moments.get(i).bar.getVolumeQuote()/sheet.moments.get(i).bar.middlePrice();
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

package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;

public class VolumeIndicator extends BaseIndicator{

    public static final Color COLOR = new Color(0,0,192);
    public static final Color COLORMIN = Color.lightGray.brighter();


    public VolumeIndicator(IndicatorInitData data){
        super(data);
    }

    @Override
    public String getName() {
        return "Volume";
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.NUMBER;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {
        for (int i = 0;i<values.length;i++)
            values[i] =  sheet.moments.get(i).bar.getVolume();;
    }

    @Override
    public Color getColorMax() {
        return COLOR;
    }

    public Color getColorMin() {        return COLORMIN;    }

    @Override
    public boolean fromZero() {
        return true;
    }
}


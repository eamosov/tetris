package ru.efreet.trading.book.indicators;

import ru.efreet.trading.book.Sheet;

import java.awt.*;

public class VolumeIndicator implements IIndicator{
    public static final int Id = 3;

    public static final Color COLOR = new Color(0,0,192);
    public static final Color COLORMIN = Color.lightGray.brighter();

    @Override
    public int getId() {
        return Id;
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


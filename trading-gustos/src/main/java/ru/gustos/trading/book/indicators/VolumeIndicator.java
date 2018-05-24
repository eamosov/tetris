package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

import java.awt.*;

public class VolumeIndicator extends NumberIndicator{

    public VolumeIndicator(IndicatorInitData data){
        super(data);
    }

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        for (int i = from;i<to;i++)
            values[0][i] =  sheet.bar(i).getVolume();
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


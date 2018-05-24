package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

import java.awt.*;

public class VolumeBaseIndicator extends NumberIndicator{


    public VolumeBaseIndicator(IndicatorInitData data){
        super(data);
    }

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        for (int i = from;i<to;i++)
            values[0][i] =  sheet.bar(i).getVolumeBase();
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


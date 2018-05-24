package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

import java.awt.*;

public class MinMaxToVolumeSIndicator extends NumberIndicator{


    public MinMaxToVolumeSIndicator(IndicatorInitData data){
        super(data);
    }


    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        for (int i = from;i<to;i++)
            values[0][i] =  Math.abs(sheet.bar(i).deltaMaxMin())/Math.max(0.1,sheet.bar(i).getVolume());
    }

    @Override
    public ColorScheme getColors() {
        return ColorScheme.WHITEBLUE;
    }
}

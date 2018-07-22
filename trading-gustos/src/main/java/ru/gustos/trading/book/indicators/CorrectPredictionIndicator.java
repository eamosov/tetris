package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.ml.DataPlayer;

public class CorrectPredictionIndicator extends Indicator{


    public CorrectPredictionIndicator(IndicatorInitData data) {
        super(data);
    }

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        try {
            System.arraycopy(new DataPlayer(sheet).dayByDay(14,false),0,values[0],0,to);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


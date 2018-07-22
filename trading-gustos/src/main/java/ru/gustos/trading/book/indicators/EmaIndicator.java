package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.ml.DataPlayer;

public class EmaIndicator extends NumberIndicator{


    public EmaIndicator(IndicatorInitData data) {
        super(data);
    }

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        System.arraycopy(VecUtils.ma(sheet.getData().get(data.ind),data.t1),0,values[0],0,to);
    }

    @Override
    public double getUpperBound() {
        return 1;
    }

    @Override
    public double getLowerBound() {
        return -1;
    }
}

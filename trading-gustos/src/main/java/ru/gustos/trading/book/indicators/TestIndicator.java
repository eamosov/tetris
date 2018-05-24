package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

import java.util.Arrays;

public class TestIndicator extends Indicator {
    private int[] ind;
    public TestIndicator(IndicatorInitData data){
        super(data);
        ind = Arrays.stream(data.indicators.split(",")).mapToInt(Integer::parseInt).toArray();
    }

    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {

        double[] d1 = sheet.getData().get(ind[0]);
        double[] d2 = sheet.getData().get(ind[1]);


        boolean in = false;
        for (int i = from; i < to; i++) {
            if (!in){
                in = d1[i]== Indicator.YES && (!data.b1 || d2[i]<0);
            } else {
                in = d1[i]== Indicator.YES || (data.b2 && d2[i]<0);
            }

            values[0][i] = in? Indicator.YES: Indicator.NO;


        }
    }

}


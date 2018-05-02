package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

import java.awt.*;
import java.util.Arrays;

public class TestIndicator extends BaseIndicator{
    private int[] ind;
    private boolean b1,b2;
    public TestIndicator(IndicatorInitData data){
        super(data);
        ind = Arrays.stream(data.indicators.split(",")).mapToInt(Integer::parseInt).toArray();
        b1 = data.b1;
        b2 = data.b2;
    }

    @Override
    public String getName() {
        return "test_"+Arrays.toString(ind);
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.YESNO;
    }

    @Override
    public Color getColorMax() {
        return Color.green;
    }

    @Override
    public Color getColorMin() {
        return Color.red;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values, int from, int to) {

        double[] d1 = sheet.getData().get(ind[0]);
        double[] d2 = sheet.getData().get(ind[1]);


        boolean in = false;
        for (int i = from; i < to; i++) {
            if (!in){
                in = d1[i]==IIndicator.YES && (!b1 || d2[i]<0);
            } else {
                in = d1[i]==IIndicator.YES || (b2 && d2[i]<0);
            }

            values[i] = in?IIndicator.YES:IIndicator.NO;


        }
    }

}


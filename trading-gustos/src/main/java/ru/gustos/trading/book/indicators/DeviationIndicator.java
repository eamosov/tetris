package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;
import java.util.Arrays;

public class DeviationIndicator extends NumberIndicator {

    int t1;

    public DeviationIndicator(IndicatorInitData data){
        super(data);
        t1 = data.t1;
    }

    @Override
    public String getName() {
        return "Deviation_"+t1;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {
        Arrays.fill(values,0);
        for (int i = t1;i<values.length;i++){
            double sum = 0;
            for (int j = 0;j<t1;j++)
                sum += sheet.moments.get(i-t1+j).bar.getClosePrice();

            sum/=t1;
            double dev = 0;
            for (int j = 0;j<t1;j++){
                double v = sheet.moments.get(i-t1+j).bar.getClosePrice()-sum;
                dev += v*v;
            }
            values[i] = (sheet.moments.get(i).bar.getClosePrice()-sum)/Math.max(1,Math.sqrt(dev/t1));
        }
    }

}

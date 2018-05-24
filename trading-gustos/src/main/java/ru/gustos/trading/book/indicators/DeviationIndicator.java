package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.visual.CandlesPane;

import java.awt.*;
import java.util.Arrays;

public class DeviationIndicator extends NumberIndicator {


    public DeviationIndicator(IndicatorInitData data){
        super(data);
    }


    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        for (int i = Math.max(from,data.t1);i<to;i++){
            double sum = 0;
            for (int j = 0;j<data.t1;j++)
                sum += sheet.moments.get(i-data.t1+j).bar.getClosePrice();

            sum/=data.t1;
            double dev = 0;
            for (int j = 0;j<data.t1;j++){
                double v = sheet.moments.get(i-data.t1+j).bar.getClosePrice()-sum;
                dev += v*v;
            }
            values[0][i] = (sheet.bar(i).getClosePrice()-sum)/Math.max(1,Math.sqrt(dev/data.t1));
        }
    }

}

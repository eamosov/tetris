package ru.gustos.trading.book.ml;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.commons.math3.transform.*;
import ru.gustos.trading.book.Sheet;

public class PredictLinear {

    public static SimpleRegression whereWillGo(double[] v){
        SimpleRegression r = new SimpleRegression();
        for (int i = 0;i<v.length;i++)
            r.addData(i,v[i]);
        return r;
    }

    public static SimpleRegression whereWillGo(Sheet sheet, int index, int window) {
        double[] v = new double[window];
        for (int i = 0;i<window;i++)
            v[i] = sheet.bar(index-window+i+1).getClosePrice();
        return whereWillGo(v);
    }

    public static SimpleRegression optimums(Sheet sheet, int index, int window, int code, int cnt) {
        SimpleRegression r = new SimpleRegression();
        int i = index;
        int cc = 0;
        do {
            if (sheet.isOptimum(i,window,code)) {
                r.addData(i, code > 0 ? sheet.bar(index).getMaxPrice() : sheet.bar(index).getMinPrice());
                cc++;
            }
            i--;
        } while (cc<cnt && i>=0);
        return r;
    }


}

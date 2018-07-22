package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.SheetUtils;

import java.awt.*;

public class HistoryMoodIndicator extends NumberIndicator {
    IndicatorPeriod period;

    public HistoryMoodIndicator(IndicatorInitData data){
        super(data);
        period =  IndicatorPeriod.values()[data.period];
    }

    @Override
    public ColorScheme getColors() {
        return data.positive?ColorScheme.GREENGRAY:ColorScheme.REDGRAY;
    }


    @Override
    public void calcValues(Sheet sheet, double[][] values, int from, int to) {
        int bars = IndicatorUtils.bars(period,sheet);


        double[] sellValues = SheetUtils.sellValues(sheet,data.positive);
        for (int i = Math.max(from,bars);i<to;i++){
            double result = run(sheet,i-bars,i-5,sellValues, data.positive);
            if (data.positive)
                values[0][i] = result/1000-1;
            else
                values[0][i] = 1000/result-1;
        }

    }

    public static final double startMoney = 1000;

    public static final double fee = 0.9995;

    public static double run(Sheet sheet, int from, int to,double[] sellValues, boolean positive){
        double money = startMoney;
        int offset = 30;
        double percent = 1.015;
        for (int i = from;i<to-offset-1;i++){
            XBar bar = sheet.bar(i);
            XBar nextbar = sheet.moments.get(i+1).bar;
            double val, nextval;
            if (positive)
                val = sellValues[i+offset]/bar.getClosePrice();
            else
                val = bar.getClosePrice()/sellValues[i+offset];
            if (positive)
                nextval = sellValues[i+1+offset]/nextbar.getClosePrice();
            else
                nextval = nextbar.getClosePrice()/sellValues[i+1+offset];

            if (val>percent && val>nextval){
                double btc = money/sheet.bar(i).getClosePrice()*fee;
                money = btc*sellValues[i+offset];
                i+=61;
            }
        }
        return money;
    }

}

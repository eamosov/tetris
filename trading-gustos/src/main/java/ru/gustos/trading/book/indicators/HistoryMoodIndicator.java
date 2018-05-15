package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.SheetUtils;
import ru.gustos.trading.bots.BotRunner;
import ru.gustos.trading.bots.CheckPeriodBot;
import ru.gustos.trading.bots.IDecisionBot;

import java.awt.*;
import java.util.Arrays;

public class HistoryMoodIndicator extends BaseIndicator {
    boolean optimist;
    IndicatorPeriod period;

    public HistoryMoodIndicator(IndicatorInitData data){
        super(data);
        optimist = data.positive;
        period =  IndicatorPeriod.values()[data.period];
    }

    @Override
    public String getName() {
        return "mood_"+(optimist?"pos":"neg")+"_"+period.name();
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.NUMBER;
    }

    @Override
    public Color getColorMax() {
        return optimist?Color.green:Color.red;
    }

    @Override
    public Color getColorMin() {
        return Color.black;
    }

//    @Override
//    public boolean fromZero() {
//        return true;
//    }

    @Override
    public void calcValues(Sheet sheet, double[] values, int from, int to) {
        int bars = IndicatorUtils.bars(period,sheet);


        double[] sellValues = SheetUtils.sellValues(sheet,optimist);
        for (int i = Math.max(from,bars);i<to;i++){
            double result = run(sheet,i-bars,i-5,sellValues);
            if (optimist)
                values[i] = result/1000-1;
            else
                values[i] = 1000/result-1;
        }

    }

    public static final double startMoney = 1000;

    public static final double fee = 0.9995;

    public double run(Sheet sheet, int from, int to,double[] sellValues){
        double money = startMoney;
        int offset = 30;
        double percent = 1.015;
        for (int i = from;i<to-offset-1;i++){
            XBar bar = sheet.bar(i);
            XBar nextbar = sheet.moments.get(i+1).bar;
            double val, nextval;
            if (optimist)
                val = sellValues[i+offset]/bar.getClosePrice();
            else
                val = bar.getClosePrice()/sellValues[i+offset];
            if (optimist)
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

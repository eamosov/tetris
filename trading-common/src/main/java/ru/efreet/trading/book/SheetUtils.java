package ru.efreet.trading.book;

import kotlin.Pair;
import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.book.indicators.Decision;
import ru.efreet.trading.book.indicators.IIndicator;
import ru.efreet.trading.book.indicators.IndicatorType;

public class SheetUtils {

    public static void FillDecisions(Sheet sheet){
        initDecisions(sheet);
//        filterBadBuys(sheet);
//        filterBadSells(sheet);
//        filterBadBuys(sheet);
//        filterBadSells(sheet);
//        filterBadBuys(sheet);
//        filterBadSells(sheet);
//        initDecisionsRisky(sheet);
    }

    private static void filterBadBuys(Sheet sheet) {
        for (int i = 0;i<sheet.moments.size();i++) {
            Moment m1 = sheet.moments.get(i);
            if (m1.decision == Decision.BUY)
                for (int j = i;j<sheet.moments.size();j++) {
                    Moment m2 = sheet.moments.get(j);
                    if (m2.decision==Decision.SELL){
                        double profit = m2.bar.getMinPrice()/m1.bar.getMaxPrice();
                        if (profit<1.004)
                            m1.decision = Decision.NONE;
                        break;
                    }
                }
        }

    }

    private static void filterBadSells(Sheet sheet) {
        for (int i = 0;i<sheet.moments.size();i++) {
            Moment m1 = sheet.moments.get(i);
            if (m1.decision == Decision.SELL)
                for (int j = i;j<sheet.moments.size();j++) {
                    Moment m2 = sheet.moments.get(j);
                    if (m2.decision==Decision.BUY){
                        double profit = m2.bar.getMinPrice()/m1.bar.getMaxPrice();
                        if (profit>1.004)
                            m1.decision = Decision.NONE;
                        break;
                    }
                }
        }

    }

    private static void initDecisions(Sheet sheet) {
        int lookNext = 10;
        for (int i = 0;i<sheet.moments.size()-lookNext;i++)
            sheet.moments.get(i).decision = CalcDecision(sheet,i,lookNext);
    }

    private static Decision CalcDecision(Sheet sheet, int from, int next) {
        int lo = 0, hi = 0;
        double now = sheet.moments.get(from).bar.middlePrice();
        for (int i = from;i<from+next;i++) {
            XBar bar = sheet.moments.get(i).bar;
            double price =  bar.middlePrice();//(bar.getOpenPrice()+bar.getClosePrice())/2;
            double loprice =  bar.getMinPrice();
            if (price>now*1.005) hi++;
            if (price<now*0.995 || loprice<now*0.98) lo++;
        }
        if (hi>=4 && lo<3) return Decision.BUY;
        if (lo>=4 && hi<3)
            return Decision.SELL;
        return Decision.NONE;
    }

    private static void initDecisionsRisky(Sheet sheet) {
        int lookNext = 5;
        for (int i = 0;i<sheet.moments.size()-lookNext;i++)
            sheet.moments.get(i).decisionRisky = CalcDecisionRisky(sheet,i,lookNext);
    }

    private static Decision CalcDecisionRisky(Sheet sheet, int from, int next) {
        int lo = 0, hi = 0;
        double now = sheet.moments.get(from).bar.middlePrice();
        for (int i = from;i<from+next;i++) {
            XBar bar = sheet.moments.get(i).bar;
            double price =  bar.middlePrice();
//            double loprice =  bar.getMinPrice();
            if (price>now*1.007) hi++;
//            if (loprice<now*0.98) lo++;
        }
        if (hi>=2 && lo<=2) return Decision.BUY;
        return Decision.NONE;
    }



    public static Pair<Double, Double> getIndicatorMinMax(Sheet sheet, IIndicator ind, int from, int to) {
        double min = ind.fromZero()?0:Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        if (ind.getType()== IndicatorType.NUMBER){
            for (int i = from; i < to; i++){
                double val = sheet.getData().get(ind,i);
                if (!Double.isNaN(val)) {
                    if (val < min) min = val;
                    if (val > max) max = val;
                }
            }
            if (!ind.fromZero()){
                double m = Math.max(Math.abs(min),Math.abs(max));
                max = m;
                min = -m;
            }
        } else {
            min = -1;
            max = 1;
        }
        return new Pair<>(min,max);
    }
}

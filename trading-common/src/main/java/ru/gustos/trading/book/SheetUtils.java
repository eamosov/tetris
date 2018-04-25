package ru.gustos.trading.book;

import kotlin.Pair;
import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.Decision;
import ru.gustos.trading.book.indicators.IIndicator;
import ru.gustos.trading.book.indicators.IndicatorType;

import java.util.Arrays;

public class SheetUtils {

    public static void FillDecisions(Sheet sheet){
        initDecisions(sheet);
        filterBadBuys(sheet);
        filterBadSells(sheet);
        filterBadBuys(sheet);
        filterBadSells(sheet);
        filterBadBuys(sheet);
        filterBadSells(sheet);
        initDecisionsRisky(sheet);
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

    private static void initDecisions2(Sheet sheet) {
        double[] sellValuesPos = sellValues(sheet, true);
        double[] sellValuesNeg = sellValues(sheet, false);
        int lookNext = 60;
        for (int i = 0;i<sheet.moments.size()-lookNext;i++) {
            XBar bar = sheet.moments.get(i).bar;
            int pos = 0;
            int neg = 0;
            for (int j = i+1;j<i+lookNext;j++){
                if (bar.getMaxPrice()*1.0075<sellValuesPos[j])
                    pos++;
                if (bar.getMinPrice()>sellValuesNeg[j])
                    neg++;
            }

            sheet.moments.get(i).decision = pos>10?Decision.BUY:(neg>10?Decision.SELL:Decision.NONE);
        }
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



    public static Pair<Double, Double> getMinMax(Sheet sheet, int from, int to) {
        double min = Double.MAX_VALUE;
        double max = 0;
        for (int i = from; i < to; i++){
            double cmin = sheet.moments.get(i).bar.getMinPrice();
            if (cmin<min)
                min = cmin;
            double cmax = sheet.moments.get(i).bar.getMaxPrice();
            if (cmax>max)
                max = cmax;
        }
        return new Pair<>(min,max);
    }

    public static Pair<Double, Double> getMinMaxClose(Sheet sheet, int from, int to) {
        double min = Double.MAX_VALUE;
        double max = 0;
        for (int i = from; i < to; i++){
            double close = sheet.moments.get(i).bar.getClosePrice();
            if (close<min)
                min = close;
            if (close>max)
                max = close;
        }
        return new Pair<>(min,max);
    }

    public static Pair<Double, Double> getIndicatorMinMax(Sheet sheet, IIndicator ind, int from, int to, int scale) {
        double min = ind.fromZero()?0:Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        if (ind.getType()== IndicatorType.NUMBER){
            for (int i = from; i < to; i+=scale){
                double val = sheet.getData().get(ind,i,scale);
                if (val < min) min = val;
                if (val > max) max = val;
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

    public static double sellPrice(Sheet sheet, int index, boolean optimist){
        XBar bar = sheet.moments.get(index).bar;
        if (optimist)
            return bar.getMinPrice()*0.8 + bar.getMaxPrice()*0.2;
        else
            return bar.getMinPrice()*0.2 + bar.getMaxPrice()*0.8;
    }

    public static double[] sellValues(Sheet sheet, boolean optimist){
        double[] sellValues = new double[sheet.moments.size()];
        for (int i = 0;i<sellValues.length;i++){
            double v = sellPrice(sheet,i,optimist);
            for (int j = i-2;j<=i+2;j++)
                if (j>=0 && j<sellValues.length){
                    double vv = sellPrice(sheet,j,optimist);
                    if (optimist && vv<v)
                        v = vv;
                    else if (!optimist && vv>v)
                        v = vv;
                }
            sellValues[i] = v;
        }
        return sellValues;
    }

    public static PlayResults playIndicator(Sheet sheet, int indicator, int from, int to) {
        double[] v = sheet.getData().get(indicator);
        double money = 1000;
        double buyCost = 0;
        double btc = 0;
        double fee = 0.0005;
        PlayResults result = new PlayResults();
        int profitable = 0;
        double bestPrice = 0;
        result.bestPossibleProfit = 1;
        result.money = new double[v.length];
        Arrays.fill(result.money,money);
        double moneyWhenBuy = 0;
        for (int i = from;i<to;i++){
            if (money>0 && v[i]!=0){
                moneyWhenBuy = money;
                bestPrice = buyCost = sheet.moments.get(i).bar.getClosePrice()/(1-fee);
                btc += money/buyCost;
                money = 0;
                result.trades++;
            } else if (btc>0){
                double sellCost = sheet.moments.get(i).bar.getClosePrice() * (1+fee);
                double min = Double.MAX_VALUE;
                for (int j = -2;j<=2;j++)
                    if (i+j>=0 && i+j<sheet.moments.size())
                        min = Math.min(min,sheet.moments.get(i+j).bar.getMinPrice() * (1+fee));
                bestPrice = Math.max(bestPrice,min);
                if (v[i]==0 || i==to-1) {
                    money += btc * sellCost;
                    btc = 0;
                    result.bestPossibleProfit*=bestPrice/sellCost;
                    if (sellCost > buyCost) {
                        profitable++;
                        result.successProfit += sellCost / buyCost;
                    } else {
                        result.looseProfit += sellCost / buyCost;
                    }
                }
            }
            result.money[i] = money==0?moneyWhenBuy:money;
        }
        if (to<result.money.length)
            Arrays.fill(result.money,to,result.money.length,money);

        result.profit = money/1000;
        result.profitable = ((double)profitable)/result.trades;
        result.successProfit/=Math.max(1,profitable);
        result.looseProfit/=Math.max(1,result.trades-profitable);
        result.bestPossibleProfit*=result.profit;
        return result;
    }

    public static class PlayResults {
        public int trades;
        public double profit;
        public double profitable;
        public double successProfit;
        public double looseProfit;
        public double bestPossibleProfit;
        public double[] money;

        public String toString(){
            return String.format("trades: %d\nprofitable %%: %.4g\nprofit: %.4g\nsuccessProfit: %.4g\nlooseProfit: %.4g\nbestPossible: %.4g", trades,profitable,profit,successProfit,looseProfit,bestPossibleProfit);
        }
    }
}

package ru.gustos.trading.book;

import kotlin.Pair;
import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.Decision;
import ru.gustos.trading.book.indicators.Indicator;
import ru.gustos.trading.book.indicators.IndicatorResultType;
import ru.gustos.trading.global.InstrumentData;

import java.util.ArrayList;

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
        for (int i = 0;i<sheet.size();i++) {
            Moment m1 = sheet.moments.get(i);
            if (m1.decision == Decision.BUY)
                for (int j = i;j<sheet.size();j++) {
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
        for (int i = 0;i<sheet.size();i++) {
            Moment m1 = sheet.moments.get(i);
            if (m1.decision == Decision.SELL)
                for (int j = i;j<sheet.size();j++) {
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
        for (int i = 0;i<sheet.size()-lookNext;i++)
            sheet.moments.get(i).decision = CalcDecision(sheet,i,lookNext);
    }

    private static void initDecisions2(Sheet sheet) {
        double[] sellValuesPos = sellValues(sheet, true);
        double[] sellValuesNeg = sellValues(sheet, false);
        int lookNext = 60;
        for (int i = 0;i<sheet.size()-lookNext;i++) {
            XBar bar = sheet.bar(i);
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
            XBar bar = sheet.bar(i);
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
        for (int i = 0;i<sheet.size()-lookNext;i++)
            sheet.moments.get(i).decisionRisky = CalcDecisionRisky(sheet,i,lookNext);
    }

    private static Decision CalcDecisionRisky(Sheet sheet, int from, int next) {
        int lo = 0, hi = 0;
        double now = sheet.moments.get(from).bar.middlePrice();
        for (int i = from;i<from+next;i++) {
            XBar bar = sheet.bar(i);
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
            double cmin = sheet.bar(i).getMinPrice();
            if (cmin<min)
                min = cmin;
            double cmax = sheet.bar(i).getMaxPrice();
            if (cmax>max)
                max = cmax;
        }
        return new Pair<>(min,max);
    }

    public static Pair<Double, Double> getMinMaxClose(Sheet sheet, int from, int to) {
        double min = Double.MAX_VALUE;
        double max = 0;
        for (int i = from; i < to; i++){
            double close = sheet.bar(i).getClosePrice();
            if (close<min)
                min = close;
            if (close>max)
                max = close;
        }
        return new Pair<>(min,max);
    }

    public static Pair<Double, Double> getIndicatorMinMax(Sheet sheet, Indicator ind, int from, int to, int scale) {
        double min = ind.fromZero()?0:Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        if (ind.getResultType()== IndicatorResultType.NUMBER){
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

    public static double sellPrice(BarsSource sheet, int index, boolean optimist){
        XBar bar = sheet.bar(index);
        if (optimist)
            return bar.getMinPrice()*0.8 + bar.getMaxPrice()*0.2;
        else
            return bar.getMinPrice()*0.2 + bar.getMaxPrice()*0.8;
    }

    public static double[] sellValues(BarsSource sheet, boolean optimist){
        double[] sellValues = new double[sheet.size()];
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

    public static double gridPrice(double price, double grid) {
        price = Math.pow(grid, (int) (Math.log(price) / Math.log(grid)));
        if (price <= 0.00000001) price = 0.00000001;
        return price;
    }

    public static String price2string(BarsSource sheet, double price) {
        double minPrice = sheet.totalBar().getMinPrice();
        double mul = 1;
        while (minPrice*mul<1000)
            mul*=10;
        while (minPrice*mul>10000)
            mul/=10;
        return Integer.toString((int)(price*mul));
    }

    public static double upCandles(BarsSource sheet,  int from, int bars){
        double r = 0;
        int cc = 0;
        for (int i = 0;i<bars;i++)if (from-i>=0){
            XBar bar = sheet.bar(from - i);
            double upper=bar.getMaxPrice()-Math.max(bar.getOpenPrice(),bar.getClosePrice());
            double total=bar.getMaxPrice()-bar.getMinPrice();
            r+= total<0.0000001?0:upper/total;
            cc++;
        }

        return r/cc;
    }

    public static double downCandles(BarsSource sheet,  int from, int bars){
        double r = 0;
        int cc = 0;
        for (int i = 0;i<bars;i++)if (from-i>=0){
            XBar bar = sheet.bar(from - i);
            double lower =Math.min(bar.getOpenPrice(),bar.getClosePrice())-bar.getMinPrice();
            double total =bar.getMaxPrice()-bar.getMinPrice();
            r+=total<0.0000001?0:lower/total;
            cc++;
        }
        return r/cc;
    }

    public static double volumesAroundLevel(BarsSource sheet, double price, int from, int bars){
        double upper = 0;
        double lower = 0;
        for (int i = 0;i<bars;i++)if (from-i>=0){
            XBar bar = sheet.bar(from - i);
            if (bar.getMaxPrice()<=price)
                lower+=bar.getVolume();
            else if (bar.getMinPrice()>=price)
                upper+=bar.getVolume();
            else {
                double k = (price-bar.getMinPrice())/bar.deltaMaxMin();
                lower+=bar.getVolume()*k;
                upper+=bar.getVolume()*(1-k);
            }
        }
        if (upper+lower==0) return 0;
        return upper/(upper+lower);
    }

    public static int findMinimum(BarsSource sheet, int from, int to){
        double p = sheet.bar(from).getMinPrice();
        int res = from;
        for (int i = from+1;i<to;i++) {
            double v = sheet.bar(i).getMinPrice();
            if (v<p) {
                p = v;
                res = i;
            }
        }
        return res;
    }

    public static int findMaximum(BarsSource sheet, int from, int to){
        double p = sheet.bar(from).getMaxPrice();
        int res = from;
        for (int i = from+1;i<to;i++) {
            double v = sheet.bar(i).getMaxPrice();
            if (v>p) {
                p = v;
                res = i;
            }
        }
        return res;
    }


    public static ArrayList<Integer> findPrevMinimums(BarsSource sheet, int index, int r, int cnt){
        return findPrevPoints(sheet, index, r, cnt, SheetUtils::findMinimum);
    }

    public static ArrayList<Integer> findPrevMaximums(BarsSource sheet, int index, int r, int cnt){
        return findPrevPoints(sheet, index, r, cnt, SheetUtils::findMaximum);
    }

    public static ArrayList<Integer> findPrevPoints(BarsSource sheet, int index, int r, int cnt, FindHelper find){
        ArrayList<Integer> result = new ArrayList<>();
        int m = find.find(sheet, index - r / 2, index + 1);
        result.add(m);
        while (result.size()<cnt){
            m = find.find(sheet, m - r, m);
            result.add(m);
        }
        return result;
    }

    public static double avgPrice(BarsSource data, int from, int cnt) {
        double s = 0;
        for (int i = from;i<from+cnt;i++)
            s+=data.bar(i).getClosePrice();
        return s/cnt;

    }

    interface FindHelper {
        int find(BarsSource sheet, int from, int to);
    }

    public static boolean isMinimum(BarsSource sheet, int index, int window){
        double here = sheet.bar(index).getMinPrice();
        for (int i = 1;i<=window;i++){
            if (index-i>=0 && sheet.bar(index-i).getMinPrice()<here) return false;
            if (index+i<sheet.size() && sheet.bar(index+i).getMinPrice()<here) return false;
        }
        return true;
    }

    public static boolean isMaximum(BarsSource sheet, int index, int window){
        double here = sheet.bar(index).getMaxPrice();
        for (int i = 1;i<=window;i++){
            if (index-i>=0 && sheet.bar(index-i).getMaxPrice()>here) return false;
            if (index+i<sheet.size() && sheet.bar(index+i).getMaxPrice()>here) return false;
        }
        return true;
    }

    public static boolean isOptimum(BarsSource sheet, int index, int window, int code){
        if (code==1) return isMaximum(sheet, index,window);
        if (code==-1) return isMinimum(sheet, index,window);
        throw new NullPointerException("unknown code "+code);
    }

}



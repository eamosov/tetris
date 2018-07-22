package ru.gustos.trading.book;

import kotlin.Pair;
import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.indicators.VecUtils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;

public abstract class PriceHistogram {
    private static final double step = 1.0002;
    protected BarsSource sheet;

    public int steps;
    protected final double logStep;
    protected final int minPow;

    protected int calcDayN = -1;
    protected int calcIndex = 0;

    double[] v1;
    double[] v2;
    ArrayList<double[]> byDays1 = new ArrayList<>();
    ArrayList<double[]> byDays2 = new ArrayList<>();
    private double[] day1;
    private double[] day2;
    private boolean needByDays = false;

    public PriceHistogram(BarsSource sheet, boolean needByDays){
        this.sheet = sheet;
        this.needByDays = needByDays;
        logStep = Math.log(step);
        minPow = (int) (Math.log(sheet.totalBar().getMinPrice()) / logStep);
        steps = (int) (Math.log(sheet.totalBar().getMaxPrice()) / logStep)-minPow+10;
        v1 = new double[steps];
        v2 = new double[steps];
        initCalc();
    }

    private void initCalc(){
        Arrays.fill(v1,0);
        Arrays.fill(v2,0);
        byDays1.clear();
        byDays2.clear();
        day1 = new double[steps];
        day2 = new double[steps];
    }

    protected int dayn(ZonedDateTime time){
        return (int)((time.toEpochSecond()-sheet.bar(0).getEndTime().toEpochSecond())/(24*3600));
    }

    public void calc(int to) {
        for (int i = calcIndex;i<=to;i++){
            XBar bar = sheet.bar(i);

            int cdayn = dayn(bar.getEndTime());
            while (cdayn>calcDayN){
                if (needByDays) {
                    byDays1.add(v1.clone());
                    byDays2.add(v2.clone());
                }
                calcDayN++;
            }

            process(i, v1, v2);
//            if (bar.isBullish())
//                addVolume(globalBuy,bar, bar.getVolume());
//            else
//                addVolume(globalSell,bar, bar.getVolume());

//            modifyBaseAndAsset(dayBase,dayAsset,bar);
            calcIndex = i+1;
        }
    }

    protected abstract void process(int index, double[] v1, double[] v2);

    public int price2pow(double price){
        int r = (int) (Math.log(price) / logStep) - minPow;
        if (r>=steps) r = steps-1;
        return r;
    }

    public double pow2price(int pow){
        return Math.exp((pow+minPow)*logStep);
    }

    public Pair<double[],double[]> getMoment(ZonedDateTime time){
        int day = dayn(time);
        double[] ddbuy = byDays1.get(day).clone();
        double[] ddsell = byDays2.get(day).clone();
        int i = sheet.getBarIndex(sheet.bar(0).getEndTime().plusDays(day));


        while (i<sheet.size() && sheet.bar(i).getEndTime().isBefore(time)){
            XBar bar = sheet.bar(i);
            process(i,ddbuy,ddsell);
            i++;
        }
        return new Pair<>(ddsell,ddbuy);

    }

    public Pair<double[],double[]> get() {
        return new Pair<>(v1,v2);
    }

    protected void add(double[] v, XBar bar, double x) {
        int powmin = price2pow(bar.getMinPrice());
        int powmax = price2pow(bar.getMaxPrice());
        x = x / (powmax - powmin + 1);
        for (int i = powmin; i <= powmax; i++)
            v[i] += x;
    }

    double[] integralsum;
    double[] integralma;
    public void prepareForIntegral(){
        Pair<double[], double[]> p = get();
        integralsum = VecUtils.add(p.getFirst(), p.getSecond(), 1);
        integralma = VecUtils.ma(integralsum, 100);

    }

    public double integral(double buyPrice, double target) {
        int from = price2pow(buyPrice);
        int to = price2pow(target);
        double res = 0;
        for (int i = from+1;i<=to;i++)
            if (i>0 && i<steps && integralma[i]!=0)
                res+=Math.abs(integralsum[i]-integralma[i-1])/integralma[i];
        return res;
    }
}

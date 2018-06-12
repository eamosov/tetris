package ru.gustos.trading.book.indicators;

import kotlin.Pair;
import ru.efreet.trading.bars.XBar;

import java.util.ArrayList;

public class GustosVolumeLevel2 {
    private EmaRecurrent volumeEma;
    private final double[] levels = new double[5000];
    private final double[] levelsUsd = new double[5000];
    private final double fPow;
    private final double descendK;
    private final double substK;

    private double pows[] = new double[5000];

    private double assetForce, usdForce, priceForce;
    private double assetPrice,usdPrice;
    private int min = 5000, max = 0;
    private ArrayList<double[]> forcemap = new ArrayList<>();

    public GustosVolumeLevel2(double descendK, double substK, double fPow){
        this.fPow = fPow;
        this.descendK = descendK;
        this.substK = substK;
        init();
    }

    private void init() {
        volumeEma = new EmaRecurrent(10);
        for (int i = 0;i<pows.length;i++)
            pows[i] = Math.pow(i,fPow);
    }

    public double feed(XBar bar) {
        if (!volumeEma.started()) {
            volumeEma.feed(bar.getVolume());
            int p = (int) bar.middlePrice() / 10;
            levels[p]+=bar.getVolume();
            levelsUsd[p]+=bar.getVolume();
            min = max = p;
            forcemap.add(new double[81]);
            return bar.middlePrice();
        }
        updateLevels(levels,bar,true);
        updateLevels(levelsUsd,bar,false);
        updateForces(bar);
        return assetPrice;
    }

    private double sigma(int x, int c, double k){
        return 1.0/(1+Math.exp(-(x-c)*k));
    }

    private double sigmasum(double[] v, int c, double k){
        double sum = 0;
        for (int i = min;i<=max;i++)
            sum+=v[i]*sigma(i,c,k);
        return sum;
    }

    private void subSigma(double[] v, int c, double k, double volume, double sigmasum) {
        for (int i = min;i<=max;i++) {
            v[i] -= v[i] * sigma(i, c, k) * volume*substK / sigmasum;
            if (v[i]<0.1)
                v[i] = 0;
        }
    }

    private void subVolume(double[] v, double volume){
        double sum = 0;
        for (int i1 = min; i1 <= max; i1++)
            sum+= v[i1];
        for (int i = min; i<= max; i++) {
            v[i] *= descendK * Math.max(0, 1 - volume * substK / Math.max(1, sum));
            if (v[i]<0.1)
                v[i] = 0;
        }
    }

    private void updateMinMax(){
        int lmin = 0, lmax = 0;
        for (int i = min;i<=max;i++){
            if (levels[i]>0 || levelsUsd[i]>0){
                if (lmin==0)
                    lmin = i;
                lmax = i;
            }
        }
        if (lmin!=0){
            min = lmin;
            max = lmax;
        }
    }

    private void updateLevels(double[] levels, XBar bar, boolean asset) {
        double[] v = levels;
        int c = (int) bar.middlePrice() / 10;
        double k = asset ? 0.02 : -0.02;
        double sigmasum = sigmasum(v, c, k);
        // если считаем ассет, то уменьшаться он должен охотнее у тех, кто купил его дешевле этой цены
        // если деньги, то уменьшаться должны у тех, у кого они появились дороже этой цены
        double volume = bar.getVolume();
        subSigma(v,c, k, Math.min(volume,sigmasum),sigmasum);
        if (volume>sigmasum){
            volume-=sigmasum;
            subVolume(v,volume);
        } else {
            VecUtils.mul(v,descendK);
        }
        updateMinMax();

        int aprice = (int)bar.getOpenPrice()/10;
        v[aprice] += volume / 3;
        if (aprice<min) min = aprice;
        if (aprice>max) max = aprice;
        aprice = (int)bar.getClosePrice()/10;
        v[aprice] += volume / 3;
        if (aprice<min) min = aprice;
        if (aprice>max) max = aprice;
        aprice = c;
        v[aprice] += volume / 3;
        if (aprice<min) min = aprice;
        if (aprice>max) max = aprice;
    }

    private void updateForces(XBar bar){
        double price = bar.getClosePrice();
        double f;
//        for (int j = min; j <= max; j++) {
//            f += f(v[j], j - ((int) price / 10));
//        }
        do {
            price += 10;
            f = 0;
            for (int j = min; j <= max; j++) {
                int d = j - ((int) price / 10);
                if (d<0)
                    f += f(levels[j], d);
                else
                    f += f(levelsUsd[j], d);
            }
        }while (f > 0);

        double aforce = 0, uforce = 0;
        do {
            price -= 10;
            f = 0;
            aforce = 0;
            for (int j = min; j <= max; j++) {
                int d = j - ((int) price / 10);
                if (d>0)
                    f += f(levels[j], d);
                else {
                    double ff = f(levelsUsd[j], d);
                    f += ff;
                    aforce += ff;
                }
            }
        } while (f < 0);
        assetPrice = price;
        assetForce = aforce;
//        price = price*1.02;
        price = bar.getClosePrice();
        f = 0;
        for (int j = min; j <= max; j++) {
            int d = j - ((int) price / 10);
            if (d>0)
                f += f(levels[j], d);
            else
                f += f(levelsUsd[j], d);
        }
        priceForce = f;

        double[] pp = new double[81];
        for (int i = -pp.length/2;i<=pp.length/2;i++){
            price = ((int)bar.middlePrice()/10)*10*(1+0.002*i);

//            f = 0;
//            for (int j = min; j <= max; j++) {
//                int d = j - ((int) price / 10);
//                if (d>0)
//                    f += f(levels[j], d);
//                else
//                    f += f(levelsUsd[j], d);
//            }
            if (i>0)
                pp[i+pp.length/2] = levels[(int)price/10];
            else
                pp[i+pp.length/2] = levelsUsd[(int)price/10];
        }
        forcemap.add(pp);

    }

    public double assetForce(){
        return assetForce;
    }

    public double[] forcemap(int index){
        return forcemap.get(index);
    }

    private double f(double v, int d) {
        if (d>=0)
            return v*pows[d];
        else
            return -v*pows[-d];
    }

    public double priceForce() {
        return priceForce;
    }
}

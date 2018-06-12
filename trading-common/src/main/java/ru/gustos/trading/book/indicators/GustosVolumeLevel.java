package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;

public class GustosVolumeLevel {
    private EmaRecurrent volumeEma;
    private final double[] levels = new double[5000];
    private final double fPow;
    private final double descendK;
    private final double substK;

    private double pows[] = new double[5000];

    private double force, price;
    private int min = 0, max = 0;

    public GustosVolumeLevel(double descendK, double substK, double fPow){
        this.fPow = fPow;
        this.descendK = descendK;
        this.substK = substK;
        init();
    }

    public GustosVolumeLevel(GustosVolumeLevel ll) {
        this.fPow = ll.fPow;
        this.descendK = ll.descendK;
        this.substK = ll.substK;
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

            return bar.middlePrice();
        }
        updateLevels(levels,bar);

        return price;
    }

    private void subVolume(double[] v, double volume){
        double sum = 0;
        for (int i1 = 0; i1 < v.length; i1++)
            sum+= v[i1];
        for (int i = 0; i< v.length; i++) {
            v[i] *= descendK * Math.max(0, 1 - volume * substK / Math.max(1, sum));
            if (v[i]<1)
                v[i] = 0;
            else {
                if (min==0)
                    min = i;
                max = i;
            }
        }
    }

    private void updateLevels(double[] levels, XBar bar) {
        double[] v = levels;
        double volume = bar.getVolume();
        min = 0;max = 0;
        subVolume(v,volume);
        int aprice = (int)bar.getOpenPrice()/10;
        v[aprice] += volume / 3;
        if (aprice<min) min = aprice;
        if (aprice>max) max = aprice;
        aprice = (int)bar.getClosePrice()/10;
        v[aprice] += volume / 3;
        if (aprice<min) min = aprice;
        if (aprice>max) max = aprice;
        aprice = (int)bar.middlePrice()/10;
        v[aprice] += volume / 3;
        if (aprice<min) min = aprice;
        if (aprice>max) max = aprice;

        double price = bar.getClosePrice();
        double f = 0;
        for (int j = min; j <= max; j++) {
            f += f(v[j], j - ((int) price / 10));
        }
        double force = 0;
        while (f > 0) {
            price += 10;
            f = 0;
            for (int j = min; j <= max; j++) {
                f += f(v[j], j - ((int) price / 10));
            }
        }
        while (f < 0) {
            price -= 10;
            f = 0;
            force = 0;
            for (int j = min; j <= max; j++) {

                int d = j - ((int) price / 10);
                double ff = f(v[j], d);
                f += ff;
                if (d>0)
                    force+=ff;
            }
        }
        this.price = price;
        this.force = force;
    }

    public double force(){
        return force;
    }

    private double f(double v, int d) {
        if (d>=0)
            return v*pows[d];
        else
            return -v*pows[-d];
    }

}





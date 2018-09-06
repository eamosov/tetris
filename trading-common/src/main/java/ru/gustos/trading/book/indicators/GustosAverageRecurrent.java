package ru.gustos.trading.book.indicators;

import kotlin.Pair;
import ru.efreet.trading.bars.XBaseBar;

import static ru.gustos.trading.book.indicators.VecUtils.ema;

public class GustosAverageRecurrent {

    private int window;
    private final EmaRecurrent volumeEma;
    private final EmaRecurrent volumeEmaShort;
    private double pvalue2;
    private double pdisp2;
    private double pvalue;
    private double pdisp;
    private double value;
    private double disp;
    private double pow1;// = 2.4;
    private double pow2;// = 1.4;


    public GustosAverageRecurrent(int window, int volumeWindow, int shortVolumeWindow) {
        this(window,volumeWindow,shortVolumeWindow,2.4,1.4);
    }
    public GustosAverageRecurrent(int window, int volumeWindow, int shortVolumeWindow, double pow1, double pow2){
        volumeEma = new EmaRecurrent(1);
        volumeEmaShort = new EmaRecurrent(1);
        changeParams(window,volumeWindow,shortVolumeWindow,pow1,pow2);
    }

    private GustosAverageRecurrent(GustosAverageRecurrent g){
        window = g.window;
        pvalue = g.pvalue;
        pdisp = g.pdisp;
        pvalue2 = g.pvalue2;
        pdisp2 = g.pdisp2;
        value = g.value;
        disp = g.disp;
        pow1 = g.pow1;
        pow2 = g.pow2;
        volumeEma = new EmaRecurrent(g.volumeEma);
        volumeEmaShort = new EmaRecurrent(g.volumeEmaShort);
    }

    public GustosAverageRecurrent clone(){
        return new GustosAverageRecurrent(this);

    }

    public void changeParams(int window, int volumeWindow, int shortVolumeWindow, double pow1, double pow2){
        if (pow1<0) pow1 = 0;
        if (pow2<0) pow2 = 0;
        if (pow1>4) pow1 = 4;
        if (pow2>4) pow2 = 4;
        this.pow1 = pow1;
        this.pow2 = pow2;
        this.window = window;
        volumeEma.changeW(volumeWindow);
        volumeEmaShort.changeW(shortVolumeWindow < 1 ? 1 : shortVolumeWindow);

    }

    public void feedNoReturn(double price, double volume){
        if (!volumeEma.started()){
            volumeEma.feed(volume);
            volumeEmaShort.feed(volume);
            pvalue = value = price;
            pdisp = disp = 0;
            return;
        }
        pvalue2 = pvalue;
        pdisp2 = pdisp;
        pvalue = value;
        pdisp = disp;
        double oldValue = value;
        double avgVolume = volumeEma.feed(volume);
        double shortVolume = volumeEmaShort.feed(volume);
        double volumek = shortVolume/Math.max(1,avgVolume);
        double a = price/oldValue;
        a*=a*=a;
        double next = oldValue+(price-oldValue)/(0.6*window*a);
        if (volumek<=1){
//            double vk = Math.pow(volumek, 5);
            double vk = volumek*volumek;
            vk = vk*vk*volumek;
            volumek = vk;
            value = oldValue * (1 - vk) + next * vk;
        } else {
            double vk = Math.pow(volumek,pow1);
            double pn = 0;
            a = price/next;
            while (vk>1) {
                pn = next;
                a*=a*=a;
                next = next + (price - next) / (0.6 * window * a);
                a = price/next;
                if (a<1.0001 && a>0.9999) {
                    vk = 0;
                    break;
                }

                vk-=1;
            }

            value = pn+vk*(next-pn);
            volumek = Math.pow(volumek,pow2);

        }
        double d = price-value;
        d*=d;
        disp = (d- disp)* 2.0/(window/ volumek +1) + disp;
    }
    public Pair<Double,Double> feed(double price, double volume){
        feedNoReturn(price,volume);
        return new Pair<>(value,Math.sqrt(disp));
    }

    public double pvalue(){
        return pvalue;
    }

    public double pvalue2(){
        return pvalue2;
    }

    public double value(){
        return value;
    }

    public double sd(){
        return Math.sqrt(disp);
    }

    public double psd(){
        return Math.sqrt(pdisp);
    }

    public double psd2(){
        return Math.sqrt(pdisp2);
    }


    public static Pair<double[], double[]> calc(double[] prices, int window, double[] volumes, int volumesWindow) {
        double[] a = new double[prices.length];
        double[] d = new double[prices.length];
        GustosAverageRecurrent gar = new GustosAverageRecurrent(window, volumesWindow, 5);
        for (int i = 0;i<prices.length;i++) {
            Pair<Double, Double> r = gar.feed(prices[i], volumes[i]);
            a[i] = r.getFirst();
            d[i] = r.getSecond();
        }
        return new Pair<>(a,d);
    }
}


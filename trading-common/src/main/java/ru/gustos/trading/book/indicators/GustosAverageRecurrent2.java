package ru.gustos.trading.book.indicators;

import kotlin.Pair;

public class GustosAverageRecurrent2 {

    private final int window;
    private final EmaRecurrent volumeEma;
    private final EmaRecurrent volumeEmaShort;
    private double value;
    private double disp;
    private double pow1;
    private double pow2;

    double[] prices;
    double[] values;
    int pos;
    int lastBigVolume;
    int avgWindow;

    double eps = 0.001;


    public GustosAverageRecurrent2(int window, int volumeWindow, int shortVolumeWindow, int avgWindow, double pow1, double pow2){
        this.window = window;
        volumeEma = new EmaRecurrent(volumeWindow);
        volumeEmaShort = new EmaRecurrent(shortVolumeWindow);
        this.avgWindow = avgWindow;
        this.pow1 = pow1;
        this.pow2 = pow2;
        prices = new double[2000000];
        values = new double[2000000];
    }

    public Pair<Double,Double> feed(double price, double volume){
        prices[pos] = price;

        if (!volumeEma.started()){
            volumeEma.feed(volume);
            volumeEmaShort.feed(volume);
            value = price;
            disp = 0;
            values[pos++] = value;
            return new Pair<>(value,disp);
        }
        double oldValue = value;
        double avgVolume = volumeEma.feed(volume);
        double shortVolume = volumeEmaShort.feed(volume);
        double volumek = shortVolume/Math.max(1,avgVolume);
        if (volumek>2) lastBigVolume = pos;
        double a = price/oldValue;
        a*=a*=a;
        double next = oldValue+(price-oldValue)/(0.6*window*a);
        if (volumek<=1){
            double vk = Math.pow(volumek, 5);
            volumek = vk;
            value = oldValue * (1 - vk) + next * vk;
        } else {
            double vk = Math.pow(volumek,pow1);
            double pn = 0;
            while (vk>1) {
                pn = next;
                a = price/next;
                a*=a*=a;
                next = next + (price - next) / (0.6 * window * a);

                vk-=1;
            }

            value = pn+vk*(next-pn);
            volumek = Math.pow(volumek,pow2);

        }
        int back = pos-lastBigVolume;
        if (back>avgWindow) back = avgWindow;
        if (back>3){
            double avg = VecUtils.avg(prices,pos-back,back);
            value = value+(avg-value)*2/(avgWindow+1);
        }
        double d = price-value;
        d*=d;
        disp = (d- disp)* 2.0/(window/ volumek +1) + disp;


        values[pos] = value;
        pos++;
        return new Pair<>(value,Math.sqrt(disp));
    }


    public static Pair<double[], double[]> calc(double[] prices, int window, double[] volumes, int volumesWindow) {
        double[] a = new double[prices.length];
        double[] d = new double[prices.length];
        GustosAverageRecurrent2 gar = new GustosAverageRecurrent2(window, volumesWindow, 5,window, 2.3, 1.4);
        for (int i = 0;i<prices.length;i++) {
            Pair<Double, Double> r = gar.feed(prices[i], volumes[i]);
            a[i] = r.getFirst();
            d[i] = r.getSecond();
        }
        return new Pair<>(a,d);
    }
}

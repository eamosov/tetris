package ru.gustos.trading.book.indicators;

import kotlin.Pair;
import ru.efreet.trading.bars.XBaseBar;

import static ru.gustos.trading.book.indicators.VecUtils.ema;

public class GustosAverageRecurrent {

    private final int window;
    private final EmaRecurrent volumeEma;
    private final EmaRecurrent volumeEmaShort;
    private double value;
    private double disp;
    private double pow1 = 2.4;
    private double pow2 = 1.4;


    public GustosAverageRecurrent(int window, int volumeWindow, int shortVolumeWindow){
        this.window = window;
        volumeEma = new EmaRecurrent(volumeWindow);
        volumeEmaShort = new EmaRecurrent(shortVolumeWindow < 1 ? 1 : shortVolumeWindow);
    }

    public Pair<Double,Double> feed(double price, double volume){
        if (!volumeEma.started()){
            volumeEma.feed(volume);
            volumeEmaShort.feed(volume);
            value = price;
            disp = 0;
            return new Pair<>(value,disp);
        }
        double oldValue = value;
        double avgVolume = volumeEma.feed(volume);
        double shortVolume = volumeEmaShort.feed(volume);
        double volumek = shortVolume/Math.max(1,avgVolume);
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
        double d = price-value;
        d*=d;
        disp = (d- disp)* 2.0/(window/ volumek +1) + disp;

//        if (volumek<=1){
//            double vk = Math.pow(volumek, 5);
//            disp = oldDisp * (1 - vk) + next * vk;
//        } else {
//            double vk = Math.pow(volumek,pow2);
//            double pn = 0;
//            while (vk>1) {
//                pn = next;
//                next = (d-next)*2.0/(window+1)+next;
//                vk-=1;
//            }
//            disp = pn+vk*(next-pn);
//        }


        return new Pair<>(value,Math.sqrt(disp));
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


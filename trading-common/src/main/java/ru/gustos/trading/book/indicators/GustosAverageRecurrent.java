package ru.gustos.trading.book.indicators;

import kotlin.Pair;
import ru.efreet.trading.bars.XBaseBar;

import static ru.gustos.trading.book.indicators.VecUtils.ema;

public class GustosAverageRecurrent {

    private final int window;
    private final EmaRecurrent volumeEma;
    private double value;
    private double disp;

    public GustosAverageRecurrent(int window, int volumeWindow){
        this.window = window;
        volumeEma = new EmaRecurrent(volumeWindow);
    }

    public Pair<Double,Double> feed(double price, double volume){
        if (!volumeEma.started()){
            volumeEma.feed(volume);
            value = price;
            disp = 0;
            return new Pair<>(value,disp);
        }
        double oldValue = value;
        double avgVolume = volumeEma.feed(volume);
        double a = price/oldValue;
        double volumek = volume/Math.max(1,avgVolume);
        a*=a;
        a*=a;
        double next = oldValue+(price-oldValue)/(0.6*window*a);
        if (volumek<=0){

        } else if (volumek<=1){
            volumek = Math.pow(volumek, 5);
            value = oldValue * (1 - volumek) + next * volumek;
        } else {
            double vk = Math.pow(volumek,2.3);
            double pn = 0;
            while (vk>1) {
                pn = next;
                next = next + (price - next) / (0.6 * window * a);
                vk-=1;
            }

            value = pn+vk*(next-pn);

        }
        double d = Math.abs(price-value);
        d=d*d;
        disp = (d-disp)* 2.0/(window/volumek+1) + disp;
        return new Pair<>(value,Math.sqrt(disp));
    }



    public static Pair<double[],double[]> gustosMcginleyAndDisp(double[] v, int t, double[] volumes, int volT) {
        double[] mc = new double[v.length];
        double[] disp = new double[v.length];
        double[] volumesAvg = ema(volumes,volT);
        mc[0] = v[0];
        disp[0] = 0;
        for (int i = 1;i<v.length;i++) {
            double a = v[i] / mc[i - 1];
            double volumek = volumes[i]/Math.max(1,volumesAvg[i]);
            a*=a;
            a*=a;
            double next = mc[i - 1] + (v[i] - mc[i - 1]) / (0.6 * t * a);
            if (volumek<=0) {
                mc[i] = mc[i-1];
            }if (volumek<=1) {
                volumek = Math.pow(volumek, 5);
                mc[i] = mc[i - 1] * (1 - volumek) + next * volumek;
            } else {
                double vk = Math.pow(volumek,2.3);
                double pn = 0;
                while (vk>1) {
                    pn = next;
                    next = next + (v[i] - next) / (0.6 * t * a);
                    vk-=1;
                }

                mc[i] = pn+vk*(next-pn);

            }
            double d = Math.abs(v[i]-mc[i]);
            d=d*d;
            disp[i] = (d-disp[i-1])* 2.0/(t/volumek+1) + disp[i-1];

        }
        for (int i = 1;i<v.length;i++)
            disp[i] = Math.sqrt(disp[i]);

        return new Pair<>(mc,disp);
    }




}


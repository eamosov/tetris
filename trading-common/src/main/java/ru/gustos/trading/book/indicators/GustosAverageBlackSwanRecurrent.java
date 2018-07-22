package ru.gustos.trading.book.indicators;

import kotlin.Pair;
import org.apache.commons.math3.stat.regression.SimpleRegression;

public class GustosAverageBlackSwanRecurrent {

    private final EmaRecurrent volumeEma;
    private final EmaRecurrent volumeEmaShort;

    private double value;
    private double disp;
    private int lastSwan;

    private double volumePike = 2.5;
    private double pauseAfterSwan = 60;

    private SimpleRegression regression = new SimpleRegression();


    public GustosAverageBlackSwanRecurrent(int volumeWindow, int shortVolumeWindow){
        volumeEma = new EmaRecurrent(volumeWindow);
        volumeEmaShort = new EmaRecurrent(shortVolumeWindow < 1 ? 1 : shortVolumeWindow);
        lastSwan = 0;
    }

    public Pair<Double,Double> feed(double price, double volume){
        if (!volumeEma.started()){
            volumeEma.feed(volume);
            volumeEmaShort.feed(volume);
            value = price;
            disp = 0;
            return new Pair<>(value,disp);
        }
        lastSwan++;
        double oldValue = value;
        double avgVolume = volumeEma.feed(volume);
        double shortVolume = volumeEmaShort.feed(volume);
        double volumek = shortVolume/Math.max(1,avgVolume);
        if (volumek>volumePike) {
            lastSwan = 0;
            regression.clear();
        } else if (lastSwan>pauseAfterSwan){
            regression.addData(lastSwan,price);
        }

        if (lastSwan>pauseAfterSwan*2){
            value = regression.predict(lastSwan);
            disp = regression.getMeanSquareError();
        } else {
            value = price;
            disp = 0;
        }
        return new Pair<>(value,Math.sqrt(disp)*1.5);
    }

    public double value(){
        return value;
    }

    public double sd(){
        return Math.sqrt(disp)*1.5;
    }


    public static Pair<double[], double[]> calc(double[] prices, double[] volumes, int volumesWindow, int volumesShort) {
        double[] a = new double[prices.length];
        double[] d = new double[prices.length];
        GustosAverageBlackSwanRecurrent gar = new GustosAverageBlackSwanRecurrent(volumesWindow, volumesShort);
        for (int i = 0;i<prices.length;i++) {
            Pair<Double, Double> r = gar.feed(prices[i], volumes[i]);
            a[i] = r.getFirst();
            d[i] = r.getSecond();
        }
        return new Pair<>(a,d);
    }

}

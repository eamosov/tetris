package ru.gustos.trading.book.indicators;

public class EmaRecurrent {

    private final double k;
    private double emap;
    private boolean started = false;

    public EmaRecurrent(int w){
        k = 2.0/(w+1);
    }

    public double feed(double v){
        if (!started){
            emap = v;
            started = true;
            return emap;
        }
        return emap = (v-emap)*k+emap;
    }

    public boolean started(){
        return started;
    }
}

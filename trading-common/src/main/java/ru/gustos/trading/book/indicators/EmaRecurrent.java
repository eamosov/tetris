package ru.gustos.trading.book.indicators;

public class EmaRecurrent {

    private double k;
    private double emap;
    private double prev;
    private boolean started = false;

    public EmaRecurrent(int w){
        changeW(w);
    }

    public EmaRecurrent(EmaRecurrent ema) {
        k = ema.k;
        emap = ema.emap;
        prev = ema.prev;
        started = ema.started;
    }

    public void changeW(int w){
        k = 2.0/(w+1);
    }

    public double feed(double v){
        if (!started){
            prev = emap = v;
            started = true;
            return emap;
        }
        prev = emap;
        return emap = (v-emap)*k+emap;
    }

    public double feed(double v, double weight){
        if (!started){
            emap = v;
            started = true;
            return emap;
        }
        prev = emap;
        return emap = (v-emap)*2.0/((2.0/k-1)/weight+1)+emap;
    }

    public boolean started(){
        return started;
    }

    public double value() {
        return emap;
    }

    public double pvalue() {
        return prev;
    }
}


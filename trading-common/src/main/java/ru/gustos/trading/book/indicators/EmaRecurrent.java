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

    public double feed(double v, double weight){
        if (!started){
            emap = v;
            started = true;
            return emap;
        }
        return emap = (v-emap)*2.0/((2.0/k-1)/weight+1)+emap;
    }

    public boolean started(){
        return started;
    }

    public double value() {
        return emap;
    }
}


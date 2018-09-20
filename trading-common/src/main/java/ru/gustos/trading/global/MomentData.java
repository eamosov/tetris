package ru.gustos.trading.global;


import java.io.Serializable;

public class MomentData implements  MomentDataProvider, Serializable {
    public double[] values;
    public long whenWillKnow;
    public double weight = 1;
    public double weight2 = 1;
    public boolean ignore = false;

    public MomentData(int size) {
        values = new double[size];
    }

    @Override
    public MomentData getMomentData() {
        return this;
    }
}


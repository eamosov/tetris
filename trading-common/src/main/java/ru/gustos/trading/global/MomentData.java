package ru.gustos.trading.global;


public class MomentData implements  MomentDataProvider{
    public double[] values;
    public long whenWillKnow;
    public double weight = 1;
    public boolean ignore = false;

    public MomentData(int size) {
        values = new double[size];
    }

    @Override
    public MomentData getMomentData() {
        return this;
    }
}


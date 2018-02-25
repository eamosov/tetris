package ru.efreet.trading.ta.indicators;

/**
 * Created by fluder on 24/02/2018.
 */
public class BarGetterSetter<B> {

    @FunctionalInterface
    public interface Setter<B> {
        void set(B bar, double value);
    }

    @FunctionalInterface
    public interface Getter<B> {
        double get(B bar);
    }

    final Setter set;
    final Getter get;

    public BarGetterSetter(Setter<B> set, Getter<B> get) {
        this.set = set;
        this.get = get;
    }

    public void set(B bar, double value) {
        set.set(bar, value);
    }

    public double get(B bar) {
        return get.get(bar);
    }
}

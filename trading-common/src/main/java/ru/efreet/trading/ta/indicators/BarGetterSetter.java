package ru.efreet.trading.ta.indicators;

/**
 * Created by fluder on 24/02/2018.
 */
public class BarGetterSetter<B> {

    @FunctionalInterface
    public interface Setter<B> {
        void set(B bar, float value);
    }

    @FunctionalInterface
    public interface Getter<B> {
        float get(B bar);
    }

    final Setter set;
    final Getter get;

    public BarGetterSetter(Setter<B> set, Getter<B> get) {
        this.set = set;
        this.get = get;
    }

    public void set(B bar, float value) {
        set.set(bar, value);
    }

    public float get(B bar) {
        return get.get(bar);
    }
}

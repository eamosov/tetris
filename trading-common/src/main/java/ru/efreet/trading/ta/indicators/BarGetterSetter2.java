package ru.efreet.trading.ta.indicators;

/**
 * Created by fluder on 24/02/2018.
 */
public class BarGetterSetter2<B, V> {

    @FunctionalInterface
    public interface Setter<B, V> {
        void set(B bar, V value);
    }

    @FunctionalInterface
    public interface Getter<B, V> {
        V get(B bar);
    }

    final Setter<B, V> set;
    final Getter<B, V> get;

    public BarGetterSetter2(Setter<B, V> set, Getter<B, V> get) {
        this.set = set;
        this.get = get;
    }

    public void set(B bar, V value) {
        set.set(bar, value);
    }

    public V get(B bar) {
        return get.get(bar);
    }
}

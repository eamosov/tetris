package ru.gustos.trading.book.indicators;

public class RsiWithVolumesRecurrent {
    EmaRecurrent greens;
    EmaRecurrent reds;

    double prev = 0;
    double value;

    public RsiWithVolumesRecurrent(int window) {
        greens = new EmaRecurrent(window);
        reds = new EmaRecurrent(window);
    }

    public double feed(double price, double volume) {
        if (prev == 0) {
            prev = price;
            value = 0;
            return 0;
        }
        double d = price - prev;
        if (d > 0)
            greens.feed(volume);
        if (d < 0)
            reds.feed(volume);


        prev = price;
        double v = reds.value() + greens.value();
        value = v == 0 ? 100 : 100 * greens.value() / v - 50;
        return value;
    }

    public double value() {
        return value;
    }
}

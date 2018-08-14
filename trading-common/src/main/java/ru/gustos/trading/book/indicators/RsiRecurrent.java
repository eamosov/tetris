package ru.gustos.trading.book.indicators;

public class RsiRecurrent {
    EmaRecurrent greens;
    EmaRecurrent reds;

    double prev = 0;
    double value;

    public RsiRecurrent(int window) {
        greens = new EmaRecurrent(window);
        reds = new EmaRecurrent(window);
    }

    public double feed(double price) {
        if (prev == 0) {
            prev = price;
            value = 0;
            return 0;
        }
        double d = price - prev;
        if (d > 0) {
            greens.feed(d);
        }
        if (d < 0) {
            reds.feed(d);
        }


        prev = price;
        double v = reds.value() + greens.value();
        value = v == 0 ? 100 : 100 * greens.value() / v - 50;
        return value;
    }

    public double value() {
        return value;
    }
}


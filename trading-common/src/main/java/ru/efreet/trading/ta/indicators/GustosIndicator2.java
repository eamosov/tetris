package ru.efreet.trading.ta.indicators;

import java.util.List;

public class GustosIndicator2<B> extends XCachedIndicator<B> {

    final int timeframe;
    final int timeframeVolumes;
    final BarGetterSetter<B> price;
    final BarGetterSetter<B> volume;
    final BarGetterSetter<B> avrPrice;
    final BarGetterSetter<B> avrVolume;

    public GustosIndicator2(List<B> bars,
                           BarGetterSetter<B> prop,
                           BarGetterSetter<B> price,
                           BarGetterSetter<B> volume,
                           BarGetterSetter<B> avrPrice,
                           BarGetterSetter<B> avrVolume,
                           int timeframe,
                           int timeframeVolumes) {
        super(bars, prop);
        this.price = price;
        this.volume = volume;
        this.avrPrice = avrPrice;
        this.avrVolume = avrVolume;
        this.timeframe = timeframe;
        this.timeframeVolumes = timeframeVolumes;
    }

    /**
     * Рекурсивное вычисление индикатора
     *
     * @param index - номер бара
     * @param bar
     * @return
     */
    @Override
    public double calculate(int index, B bar) {

        if (index == 0) {
            avrVolume.set(bar, volume.get(bar));
            avrPrice.set(bar, price.get(bar));
            return 0;
        }

        double prevDisp = getValue(index - 1);
        prevDisp = prevDisp * prevDisp;

        final List<B> bars = getBars();

        double price = this.price.get(bar);
        double volume = this.volume.get(bar);
        double prevAvgPrice = avrPrice.get(bars.get(index - 1));
        double prevAvgVolume = avrVolume.get(bars.get(index - 1));

        double avgVolume = (volume - prevAvgVolume) * 2 / (1 + timeframeVolumes) + prevAvgVolume;
        double avgPrice;

        double volumek = volume / Math.max(1, avgVolume);
        double a = price / prevAvgPrice;
        a *= a;
        a *= a;
        double next = prevAvgPrice + (price - prevAvgPrice) / (0.6 * timeframe * a);
        if (volumek <= 1) {
            volumek = Math.pow(volumek, 5);
            avgPrice = prevAvgPrice * (1 - volumek) + next * volumek;
        } else {
            double vk = volumek;
            double pn = 0;
            while (vk > 1) {
                pn = next;
                next = next + (price - next) / (0.6 * timeframe * a);
                vk -= 1;
            }
            avgPrice = pn * (1 - volumek) + next * volumek;

        }
        double d = price - avgPrice;
        d = d * d;
        double avgDisp = (d - prevDisp) * 2 / (1 + timeframe/volumek) + prevDisp;

        avrVolume.set(bar, avgVolume);
        avrPrice.set(bar, avgPrice);

        //Возвращаем текущее значение индикатора на баре index (размер коридора)
        return Math.sqrt(avgDisp);
    }

}

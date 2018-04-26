package ru.efreet.trading.ta.indicators;

import java.util.List;

/**
 * Created by fluder on 26/04/2018.
 */
public class GustosIndicator<B> extends XCachedIndicator<B> {

    final int timeframe;
    final int timeframeVolumes;
    final BarGetterSetter<B> price;
    final BarGetterSetter<B> volume;
    final BarGetterSetter<B> avrPrice;
    final BarGetterSetter<B> avrVolume;
    final BarGetterSetter<B> avrDispSquared;

    public GustosIndicator(List<B> bars,
                           BarGetterSetter<B> prop,
                           BarGetterSetter<B> price,
                           BarGetterSetter<B> volume,
                           BarGetterSetter<B> avrPrice,
                           BarGetterSetter<B> avrVolume,
                           BarGetterSetter<B> avrDispSquared,
                           int timeframe, int timeframeVolumes) {
        super(bars, prop);
        this.timeframe = timeframe;
        this.timeframeVolumes = timeframeVolumes;
        this.price = price;
        this.volume = volume;
        this.avrPrice = avrPrice;
        this.avrVolume = avrVolume;
        this.avrDispSquared = avrDispSquared;
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

        final List<B> bars = getBars();

        double price = this.price.get(bar);
        double volume = this.volume.get(bar);
        double prevAvgPrice = avrPrice.get(bars.get(index - 1));
        double prevAvgVolume = avrVolume.get(bars.get(index - 1));
        double prevDisp = avrDispSquared.get(bars.get(index - 1));

        double avgVolume = (volume - prevAvgVolume) * 2 / (1 + timeframeVolumes) + prevAvgVolume;
        double avgPrice, avgDisp;

        double volumek = volume/Math.max(1,avgVolume);
        double a = price / prevAvgPrice;
        a*=a;
        a*=a;
        double next = prevAvgPrice + (price - prevAvgPrice) / (0.6 * timeframe * a);
        if (volumek<=1)
            avgPrice = prevAvgPrice*(1-volumek)+next*volumek;
        else {
            double vk = volumek;
            double pn = 0;
            while (vk>1) {
                pn = next;
                next = next + (price - next) / (0.6 * timeframe * a);
                vk-=1;
            }
            avgPrice = pn*(1-volumek)+next*volumek;

        }
        double d = Math.abs(price-avgPrice);
        d=d*d;
        avgDisp = (d-prevDisp)*2/(1+timeframe) + prevDisp;

        avrVolume.set(bar, avgVolume);
        avrPrice.set(bar,avgPrice);
        avrDispSquared.set(bar,avgDisp);


        //Возвращаем текущее значение индикатора на баре index (размер коридора)
        return 0;
    }

    /**
     * Нерекурсивное вычисление индикатора
     * Фактически тоже самое, что и calculate(index, bar), просто разворачиваем рекурсию и оптимизируем где есть возможность....
     * Тут есть возможность хранить временные значения внутри функции, тогда как в рекурсивной процедуре все временные параметры мы вынуждены хранить в самом баре
     */
    @Override
    public void prepare() {

        final List<B> bars = getBars();

        for (int index = 0; index < getBars().size(); index++) {

            //текущий бар
            final B bar = bars.get(index);

            //текущая цена
            System.out.println(price.get(bar));

            //текущий объем
            System.out.println(volume.get(bar));

            //Средняя цена на баре index -1
            System.out.println(avrPrice.get(bars.get(index - 1)));

            //Средний объем на баре index -1
            System.out.println(avrVolume.get(bars.get(index - 1)));

            //Средний коридор на баре index -1
            System.out.println(getPropValue(bars.get(index - 1)));

            //Записываем среднюю цену
            avrPrice.set(bar, 0.0);

            //Записываем средний объем
            avrVolume.set(bar, 0.0);

            //Записываем значение индикатора ( коридор)
            setPropValue(bar, 0.0);
        }
    }
}

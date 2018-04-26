package ru.efreet.trading.ta.indicators;

import java.util.List;

/**
 * Created by fluder on 26/04/2018.
 */
public class GustosIndicator<B> extends XCachedIndicator<B> {

    final int timeframe;
    final BarGetterSetter<B> price;
    final BarGetterSetter<B> volume;
    final BarGetterSetter<B> avrPrice;
    final BarGetterSetter<B> avrVolume;

    public GustosIndicator(List<B> bars,
                           BarGetterSetter<B> prop,
                           BarGetterSetter<B> price,
                           BarGetterSetter<B> volume,
                           BarGetterSetter<B> avrPrice,
                           BarGetterSetter<B> avrVolume,
                           int timeframe) {
        super(bars, prop);
        this.timeframe = timeframe;
        this.price = price;
        this.volume = volume;
        this.avrPrice = avrPrice;
        this.avrVolume = avrVolume;
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

        //timeframe - просто константа - параметр индикатора, можно добавить ещё
        System.out.println(timeframe);

        //Предыдущие значение индикатора(размер коридора)
        double prevValue = getValue(index - 1);

        //Цена на баре index
        System.out.println(price.get(bar));

        //Объем на баре index
        System.out.println(volume.get(bar));

        //Цена на баре index - 1
        System.out.println(price.get(bars.get(index - 1)));

        //Объем на баре index - 1
        System.out.println(volume.get(bars.get(index - 1)));

        //Средняя цена на баре index -1
        System.out.println(avrPrice.get(bars.get(index - 1)));

        //Средний объем на баре index -1
        System.out.println(avrVolume.get(bars.get(index - 1)));

        //Записать среднюю цену на баре index
        avrPrice.set(bar, 0.0);

        //Записать средний объем на баре index
        avrVolume.set(bar, 0.0);

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

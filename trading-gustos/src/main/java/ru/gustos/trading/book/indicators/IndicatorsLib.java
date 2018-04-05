package ru.gustos.trading.book.indicators;

import java.util.Arrays;
import java.util.Comparator;

public class IndicatorsLib {
    private IIndicator[] indicators;
    private IIndicator[] map;

    public IndicatorsLib(){

        indicators = new IIndicator[]{new VolumeIndicator(),new TargetBuyIndicator(),new TargetSellIndicator(), new MacdIndicator(), new DemaIndicator(),
        new PriceChangeIndicator(IndicatorPeriod.TENMINUTES),new PriceChangeIndicator(IndicatorPeriod.HOUR),new PriceChangeIndicator(IndicatorPeriod.DAY),
                new PriceChangeIndicator(IndicatorPeriod.WEEK),new PriceChangeIndicator(IndicatorPeriod.MONTH),
        new RelativePriceIndicator(IndicatorPeriod.TENMINUTES),new RelativePriceIndicator(IndicatorPeriod.HOUR),new RelativePriceIndicator(IndicatorPeriod.DAY),
                new RelativePriceIndicator(IndicatorPeriod.WEEK),
        new RelativeVolumeIndicator(IndicatorPeriod.TENMINUTES),new RelativeVolumeIndicator(IndicatorPeriod.HOUR),new RelativeVolumeIndicator(IndicatorPeriod.DAY),
                new RelativeVolumeIndicator(IndicatorPeriod.WEEK),
//          new HighCandlesIndicator(IndicatorPeriod.HOUR,true),new HighCandlesIndicator(IndicatorPeriod.DAY,true), new HighCandlesIndicator(IndicatorPeriod.WEEK,true),
//          new HighCandlesIndicator(IndicatorPeriod.HOUR,false),new HighCandlesIndicator(IndicatorPeriod.DAY, false), new HighCandlesIndicator(IndicatorPeriod.WEEK,false),
        };

        Arrays.sort(indicators, Comparator.comparingInt(IIndicator::getId));
        map = new IIndicator[Arrays.stream(indicators).mapToInt(IIndicator::getId).max().getAsInt()+1];
        for (IIndicator i : indicators)
            map[i.getId()] = i;
    }

    public IIndicator[] listIndicators(){
        return indicators;
    }

    public IIndicator get(int id) {
        return map[id];
    }
}

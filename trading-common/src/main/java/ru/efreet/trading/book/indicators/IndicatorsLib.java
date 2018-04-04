package ru.efreet.trading.book.indicators;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

public class IndicatorsLib {
    private IIndicator[] indicators;
    private IIndicator[] map;

    public IndicatorsLib(){

        indicators = new IIndicator[]{new VolumeIndicator(),new TargetBuyIndicator(),new TargetSellIndicator(), new MacdIndicator(), new DemaIndicator()};
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

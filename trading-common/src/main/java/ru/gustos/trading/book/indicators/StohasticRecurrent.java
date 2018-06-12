package ru.gustos.trading.book.indicators;

import ru.efreet.trading.Decision;
import ru.efreet.trading.bars.XBar;

import java.util.Arrays;

public class StohasticRecurrent {
    private double[] maxprices;
    private double[] minprices;
    private int pos = -1;
    private double topPercent;
    private double bottomPercent;
    private double topPercentLimit;
    private double bottomPercentLimit;
    private double prevPercent;

    public StohasticRecurrent(int window, int topPercent, int bottomPercent, int topPercentLimit, int bottomPercentLimit){
        maxprices = new double[window];
        minprices = new double[window];
        this.topPercent = topPercent/100.0;
        this.bottomPercent = bottomPercent/100.0;
        this.topPercentLimit = topPercentLimit/100.0;
        this.bottomPercentLimit = bottomPercentLimit/100.0;
    }

    public Decision feed(XBar bar){
        if (pos<0){
            Arrays.fill(maxprices,bar.getMaxPrice());
            Arrays.fill(minprices,bar.getMinPrice());
            pos = 0;
            return Decision.NONE;
        }
        pos = (pos+1)%maxprices.length;
        maxprices[pos] = bar.getMaxPrice();
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (int i = 0;i<maxprices.length;i++) if (i!=pos){
            double v = maxprices[i];
            if (v<min) min = v;
            if (v>max) max = v;
        }
        double percent = (bar.getClosePrice()-min)/(max-min);
        Decision result = Decision.NONE;
        if (prevPercent>topPercent && percent<topPercent && percent>topPercentLimit)
            result = Decision.SELL;

        if (prevPercent<bottomPercent && percent>bottomPercent && percent<bottomPercentLimit)
            result = Decision.BUY;
        prevPercent = percent;
        return result;
    }

}

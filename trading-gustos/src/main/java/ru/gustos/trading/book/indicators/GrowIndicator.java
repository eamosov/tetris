package ru.gustos.trading.book.indicators;

import ru.gustos.trading.book.Sheet;

import java.awt.*;

public class GrowIndicator extends BaseIndicator{
    private int t1;
    public GrowIndicator(IndicatorInitData data){
        super(data);
        t1 = data.t1;
    }

    @Override
    public String getName() {
        return "grow_"+t1;
    }

    @Override
    public IndicatorType getType() {
        return IndicatorType.YESNO;
    }

    @Override
    public Color getColorMax() {
        return Color.green;
    }

    @Override
    public Color getColorMin() {
        return Color.red;
    }

    @Override
    public void calcValues(Sheet sheet, double[] values) {

        double[] v = sheet.moments.stream().mapToDouble(m -> m.bar.middlePrice()).toArray();
        double[] ema = VecUtils.ema(v, t1);

        boolean buy = false;
        double buyPrice = 0;
        double needChange = 0.002;
        for (int i = 2; i < values.length; i++) {
            double now = sheet.moments.get(i).bar.getClosePrice();
            if (buy){
                if ((ema[i]<ema[i-1] && now/buyPrice>(1+needChange/2)) || now/buyPrice<(1-needChange)){
                    buy = false;
                }
            } else {
                if (ema[i]>ema[i-1] && ema[i-1]<ema[i-2]){
                    buy = true;
                    buyPrice = Math.min(sheet.moments.get(i).bar.getOpenPrice(),now);
                }
            }

            if (buy)
                values[i] =IIndicator.YES;
            else
                values[i] =IIndicator.NO;
        }
    }

}

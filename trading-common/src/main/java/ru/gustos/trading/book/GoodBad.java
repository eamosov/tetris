package ru.gustos.trading.book;

import kotlin.Pair;
import ru.efreet.trading.Decision;
import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.indicators.VecUtils;

import java.time.ZonedDateTime;

public class GoodBad extends PriceHistogram {


    public GoodBad(Sheet sheet, boolean calc) {
        super(sheet,true);
        SheetUtils.FillDecisions(sheet);

        if (calc)
            calc(sheet.size() - 1);
    }


    @Override
    protected void process(int index, double[] v1, double[] v2) {
        XBar bar = sheet.bar(index);
        double close = bar.getClosePrice();
        int p = price2pow(close);
        boolean f = false;
        for (int i = 0; i < 4; i++)
            if (i + index >= 0 && i + index < sheet.size() && sheet.bar(i + index).getMinPrice() < sheet.bar(index).getMinPrice()) {
                f = true;
                break;
            }
        if (!f)
            v2[p] += 1;
        v1[p] += 1;
//        if (!f) {
//            double max = 0;
//            double min = Double.MAX_VALUE;
//            for (int i = index+1;i<Math.min(sheet.size(),index+50);i++) {
//                max = Math.max(max, sheet.bar(i).getMaxPrice());
//                min = Math.min(min, sheet.bar(i).getMinPrice());
//            }
//            double up = max - close;
//            double down = close - min;
//
//
//            v2[p] += (max/close);//*(min/close);//up / (up+down);
//            v1[p] +=1;
//        }
//        if (sheet.moments.get(index).decision== Decision.BUY)
//            v1[p]+=1;
//        if (sheet.moments.get(index).decision== Decision.SELL)
//            v2[p]+=1;

    }

    @Override
    public Pair<double[], double[]> getMoment(ZonedDateTime time) {
        Pair<double[], double[]> global = get();
        Pair<double[], double[]> m = super.getMoment(time);
        for (int i = 0; i < steps; i++) {
            double[] s = m.getSecond();
            double[] f = m.getFirst();
            f[i] = global.getFirst()[i]-f[i];
            s[i] = global.getSecond()[i]-s[i];
            if (s[i] > 0) {
                f[i] = f[i] / s[i];
                s[i] = 0;
            }
        }
        return new Pair<>(VecUtils.ma(m.getFirst(), 10), m.getSecond());
    }
}

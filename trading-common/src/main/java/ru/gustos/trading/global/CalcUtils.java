package ru.gustos.trading.global;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.indicators.GustosAverageRecurrent;

public class CalcUtils {

    public static boolean gustosSell(InstrumentData data, int index, GustosAverageRecurrent sellGar, GustosLogicOptimizator.Params params) {
        if (index == 0) return false;
        XBar pbar = data.bar(index - 1);
        XBar bar = data.bar(index);
        double sma = sellGar.value();
        double sd = sellGar.sd();
//        double p = sma - sd * values.gustosParams.sellDiv()*0.1;
        return /*bar.getMaxPrice() >= p && */bar.getClosePrice() > sma + sd * params.sellBoundDiv() * 0.1 && pbar.getClosePrice() >= bar.getMinPrice();
    }

    public static boolean gustosBuy(InstrumentData data, int index, GustosAverageRecurrent buyGar, GustosLogicOptimizator.Params params) {
        if (index == 0) return false;
        XBar pbar = data.bar(index - 1);
        XBar bar = data.bar(index);

        double p = buyGar.pvalue() - buyGar.psd() * params.buyDiv() * 0.1;
        return bar.getMinPrice() <= p && bar.getMaxPrice() >= p && bar.getClosePrice() < buyGar.value() - buyGar.sd() * params.buyBoundDiv() * 0.1 && pbar.getClosePrice() < bar.getMaxPrice();
    }

    public static boolean gustosSellEasy(InstrumentData data, int index, GustosAverageRecurrent sellGar, GustosLogicOptimizator.Params params) {
        if (index == 0) return false;
        XBar bar = data.bar(index);
        double sma = sellGar.value();
        double sd = sellGar.sd();
        return bar.getClosePrice() > sma + sd * params.sellBoundDiv() * 0.1;
    }

    public static boolean gustosBuyEasy(InstrumentData data, int index, GustosAverageRecurrent buyGar, GustosLogicOptimizator.Params params) {
        if (index == 0) return false;
        XBar bar = data.bar(index);

        return bar.getClosePrice() < buyGar.value() - buyGar.sd() * params.buyBoundDiv() * 0.1;
    }

}

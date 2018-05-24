package ru.gustos.trading.book.ml;

import kotlin.Pair;
import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.bot.TradeHistory;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.logic.ProfitCalculator;
import ru.gustos.trading.GustosBotLogicParams;
import ru.gustos.trading.book.Sheet;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LogicUtils {

    public static TradeHistory doLogic(Sheet sheet, GustosBotLogicParams args, ZonedDateTime t1, ZonedDateTime t2) {
        BarInterval barInterval = BarInterval.ONE_MIN;
        String logic = "gustos2";
        ArrayList<Pair<ZonedDateTime, ZonedDateTime>> aa = new ArrayList<>();
        aa.add(new Pair<>(t1, t2));
        ZonedDateTime after = t1.minusDays(14);
        ZonedDateTime before = t2.plusHours(1);
        List<XBar> bars = sheet.moments.stream()
                                       .filter(m -> m.bar.getBeginTime().isAfter(after) && m.bar.getBeginTime()
                                                                                                .isBefore(before))
                                       .map(m -> m.bar)
                                       .collect(Collectors.toList());
        return new ProfitCalculator().tradeHistory(logic, args, sheet.instrument(), barInterval, sheet.exchange()
                                                                                                      .getFee(), bars, aa, false);
    }

    public static double shake(Sheet sheet, GustosBotLogicParams args, double k, ZonedDateTime t1, ZonedDateTime t2) {
        Method[] fld = args.getClass().getDeclaredMethods();
        double profit = 0;
        int cc = 0;
        for (int j = -1; j <= 1; j += 2) {
            for (Method f : fld) {
                if (f.getReturnType() == Integer.class && f.getName().startsWith("get")) {
                    cc++;
                    GustosBotLogicParams p = args.copyIt();
                    try {
                        int ii = (Integer) f.invoke(p);
                        ii = (int) (1 + k * j) * ii;
                        args.getClass()
                            .getDeclaredMethod("set" + f.getName().substring(3), Integer.class)
                            .invoke(p, ii);
                        profit += doLogic(sheet, p, t1, t2).getProfitPerDay();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return profit / cc;
    }


    public static void main(String[] args) {
    }
}

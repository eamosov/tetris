package ru.gustos.trading.book.ml;

import kotlin.Pair;
import ru.efreet.trading.bars.XExtBar;
import ru.efreet.trading.bot.TradeHistory;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Instrument;
import ru.efreet.trading.logic.AbstractBotLogic;
import ru.efreet.trading.logic.ProfitCalculator;
import ru.gustos.trading.GustosBotLogic2;
import ru.gustos.trading.TestUtils;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.indicators.*;
import ru.gustos.trading.visual.Visualizator;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class ProfitChecker {
    static Sheet sheet;

    public static ZonedDateTime[] times = new ZonedDateTime[]{
            ZonedDateTime.of(2018,1,10,0,0,0,0, ZoneId.systemDefault()),
            ZonedDateTime.of(2018,2,16,0,0,0,0, ZoneId.systemDefault()),

            ZonedDateTime.of(2018,2,16,0,0,0,0, ZoneId.systemDefault()),
            ZonedDateTime.of(2018,4,10,0,0,0,0, ZoneId.systemDefault()),

            ZonedDateTime.of(2018,4,20,0,0,0,0, ZoneId.systemDefault()),
            ZonedDateTime.of(2018,5,7,0,0,0,0, ZoneId.systemDefault()),

            ZonedDateTime.of(2018,5,1,0,0,0,0, ZoneId.systemDefault()),
            ZonedDateTime.of(2018,5,8,15,0,0,0, ZoneId.systemDefault()),


    };

    private static void printTest(Instrument instr, String properties) throws Exception {
        sheet = TestUtils.makeSheet("indicators_simple.json", instr);
        AbstractBotLogic botLogic;
        BarInterval barInterval = BarInterval.ONE_MIN;
            botLogic = new GustosBotLogic2("gustos2",
                instr,
                    barInterval,
                sheet.moments.stream()
                        .map(m -> new XExtBar(m.bar))
                        .collect(Collectors.toList()));

        botLogic.loadState(properties);
        ArrayList<Pair<ZonedDateTime,ZonedDateTime>> aa = new ArrayList<>();
        ZonedDateTime t1 = times[0];
        ZonedDateTime t2 = times[times.length - 1];
        aa.add(new Pair<>(t1, t2));
        TradeHistory history = new ProfitCalculator().tradeHistory("gustos2", botLogic.getParams(), botLogic.getInstrument(), barInterval, sheet.exchange().getFee(), botLogic.getBars(), aa, false);
        out(String.format("profit interval whole: %.3g%% (pure usd %.3g%%)", (history.profitPerDayToGrow()-1)*100,(history.profitPerDay()-1)*100));

        for (int i = 0;i<times.length;i+=2){
            aa = new ArrayList<>();
            aa.add(new Pair<>(times[i],times[i+1]));
            history = new ProfitCalculator().tradeHistory("gustos2", botLogic.getParams(), botLogic.getInstrument(), barInterval, sheet.exchange().getFee(), botLogic.getBars(), aa, false);
            out(String.format("profit interval %d: %.3g%% (pure usd %.3g%%)", i/2,(history.profitPerDayToGrow()-1)*100,(history.profitPerDay()-1)*100));
        }


    }

    private static void out(String s) throws FileNotFoundException {
        System.out.println(s);
        try (PrintStream out = new PrintStream(new FileOutputStream("profitchecker.txt",true))){
            out.println(s);
            out.flush();
        }
    }

    private static void show(Sheet sheet, String properties) {
        IndicatorInitData data = new IndicatorInitData();
        data.id = 400;
        data.logic = "gustos2";
        data.state = properties;
        data.show = false;
        sheet.getLib().add(new GustosIndicator(data));
        data = new IndicatorInitData();
        data.id = 401;
        data.ind = 400;
        sheet.getLib().add(new SuccessIndicator(data));

        data = new IndicatorInitData();
        data.ind = 400;

        data.id = 410;
        data.param = "sd-";
        sheet.getLib().add(new Gustos3NumberIndicator(data));
        data.id = 412;
        data.param = "sdSell+";
        sheet.getLib().add(new Gustos3NumberIndicator(data));
        data.id = 411;
        data.param = "sd-3";
        sheet.getLib().add(new Gustos3NumberIndicator(data));
        data.id = 413;
        data.param = "sdSell+3";
        sheet.getLib().add(new Gustos3NumberIndicator(data));
        data.id = 414;
        data.param = "sma";
        sheet.getLib().add(new Gustos3NumberIndicator(data));
        data.id = 415;
        data.param = "smaSell";
        sheet.getLib().add(new Gustos3NumberIndicator(data));

        sheet.calcIndicators();

        new Visualizator(sheet);

    }

    public static void main(String[] args) throws Exception {
        out("");
        out(ZonedDateTime.now().toString());
//        out("BNB:");
//        printTest(Instrument.Companion.getBNB_USDT(), "gustoslogic_bnb.properties");
//        out("BCC:");
//        printTest(Instrument.Companion.getBCC_USDT(), "gustoslogic_bcc.properties");
        out("BTC:");
        String properties = "gustoslogic2_fine.properties";
        printTest(Instrument.Companion.getBTC_USDT(), properties);
//        out("BTCi:");
//        printTest(Instrument.Companion.getBTC_USDT(), "gustoslogic2i.properties");
        sheet = TestUtils.makeSheet("indicators_simple.json");
        show(sheet,properties);

    }

}

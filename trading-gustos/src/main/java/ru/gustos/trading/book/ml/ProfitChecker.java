package ru.gustos.trading.book.ml;

import kotlin.Pair;
import ru.efreet.trading.bars.XExtBar;
import ru.efreet.trading.bot.TradeHistory;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Instrument;
import ru.efreet.trading.exchange.TradeRecord;
import ru.efreet.trading.logic.AbstractBotLogic;
import ru.efreet.trading.logic.ProfitCalculator;
import ru.efreet.trading.logic.impl.LogicFactory;
import ru.gustos.trading.TestUtils;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.indicators.*;
import ru.gustos.trading.visual.Visualizator;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ProfitChecker {
    static Sheet sheet;

    public static ZonedDateTime[] times = new ZonedDateTime[]{
            ZonedDateTime.of(2017,11,1,0,0,0,0, ZoneId.systemDefault()),
            ZonedDateTime.of(2018,1,10,0,0,0,0, ZoneId.systemDefault()),

            ZonedDateTime.of(2018,1,10,0,0,0,0, ZoneId.systemDefault()),
            ZonedDateTime.of(2018,2,16,0,0,0,0, ZoneId.systemDefault()),

            ZonedDateTime.of(2018,2,16,0,0,0,0, ZoneId.systemDefault()),
            ZonedDateTime.of(2018,4,10,0,0,0,0, ZoneId.systemDefault()),

            ZonedDateTime.of(2018,4,20,0,0,0,0, ZoneId.systemDefault()),
            ZonedDateTime.of(2018,5,11,11,0,0,0, ZoneId.systemDefault()),

            ZonedDateTime.of(2018,5,1,0,0,0,0, ZoneId.systemDefault()),
            ZonedDateTime.of(2018,5,25,0,0,0,0, ZoneId.systemDefault()),

            ZonedDateTime.of(2018,5,20,0,0,0,0, ZoneId.systemDefault()),
            ZonedDateTime.of(2018,6,10,0,0,0,0, ZoneId.systemDefault()),

            ZonedDateTime.of(2018,6,10,0,0,0,0, ZoneId.systemDefault()),
            ZonedDateTime.of(2018,7,5,0,0,0,0, ZoneId.systemDefault()),


    };

    private static void printTest(Instrument instr, String logic, String properties) throws Exception {
        sheet = TestUtils.makeSheet("indicators_simple.json", instr);
        double lastvol = sheet.lastDayAvgVolume();
        System.out.println(String.format("\ntesting %s. volumes per day: %g", instr, lastvol));
//        if (lastvol<1 || !instr.getBase().equals("BTC"))
//            return;
        AbstractBotLogic<Object> botLogic;
        BarInterval barInterval = BarInterval.ONE_MIN;
            botLogic = (AbstractBotLogic<Object>)LogicFactory.Companion.getLogic(logic,
                instr,
                    barInterval,
                sheet.moments.stream()
                        .map(m -> new XExtBar(m.bar))
                        .collect(Collectors.toList()), false);

        botLogic.loadState(properties);
        ArrayList<Pair<ZonedDateTime,ZonedDateTime>> aa = new ArrayList<>();
        ZonedDateTime t1 = times[0];
        ZonedDateTime t2 = times[times.length - 1];
        aa.add(new Pair<>(t1, t2));
        TradeHistory history = new ProfitCalculator().tradeHistory(logic, botLogic.getParams(), botLogic.getInstrument(), barInterval, sheet.exchange().getFee(), botLogic.getBars(), aa, false);
        out(String.format("profit interval whole: %.3g%% (pure usd %.3g%%)", (history.getProfitPerDayToGrow()-1)*100,(history.getProfitPerDay()-1)*100));
        String s = history.profitString();
        out(s);
        addProfitToSet(history);

        for (int i = 0;i<times.length;i+=2){
            aa = new ArrayList<>();
            aa.add(new Pair<>(times[i],times[i+1]));
            history = new ProfitCalculator().tradeHistory(logic, botLogic.getParams(), botLogic.getInstrument(), barInterval, sheet.exchange().getFee(), botLogic.getBars(), aa, false);
            out(String.format("profit interval %d: %.3g%% (pure usd %.3g%%)", i/2,(history.getProfitPerDayToGrow()-1)*100,(history.getProfitPerDay()-1)*100));
            out(history.profitString());
        }


    }

    private static void addProfitToSet(TradeHistory history) {
        List<TradeRecord> trades = history.getTrades();

        for (int i = 10;i<trades.size();i+=2){
            double[] d = new double[6];
            for (int j = 0;j<5;j++){
                double p = trades.get(i-2-j*2).before()/trades.get(i-1-j*2).after();
                d[j] = (p-1)*100;
            }
            double p = trades.get(i).before() / trades.get(i + 1).after();
            d[5] = p>1?1:0;
            set.add(new DenseInstance(p>1?p-1:(1/p-1),d));
        }
    }

    private static void out(String s) throws FileNotFoundException {
        System.out.println(s);
        try (PrintStream out = new PrintStream(new FileOutputStream("profitchecker.txt",true))){
            out.println(s);
            out.flush();
        }
    }

    private static void show(Sheet sheet, String logic, String properties) {
        IndicatorInitData data = new IndicatorInitData();
        data.id = 400;
        data.logic = logic;
        data.state = properties;
        data.show = false;
        sheet.getLib().add(new GustosIndicator(data));
        data = new IndicatorInitData();
        data.id = 401;
        data.ind = 400;
        sheet.getLib().add(new SuccessIndicator(data));
        data = new IndicatorInitData();
        data.id = 402;
        data.ind = 400;
        sheet.getLib().add(new LevelsLogicLineIndicator(data));
        data = new IndicatorInitData();
        data.id = 404;
        data.ind = 400;
        sheet.getLib().add(new LevelsLogicHighIndicator(data));

//        data = new IndicatorInitData();
//        data.ind = 400;
//
//        data.id = 410;
//        data.param = "sd-";
//        sheet.getLib().add(new Gustos3NumberIndicator(data));
//        data.id = 412;
//        data.param = "sdSell+";
//        sheet.getLib().add(new Gustos3NumberIndicator(data));
//        data.id = 411;
//        data.param = "sd-3";
//        sheet.getLib().add(new Gustos3NumberIndicator(data));
//        data.id = 413;
//        data.param = "sdSell+3";
//        sheet.getLib().add(new Gustos3NumberIndicator(data));
//        data.id = 414;
//        data.param = "sma";
//        sheet.getLib().add(new Gustos3NumberIndicator(data));
//        data.id = 415;
//        data.param = "smaSell";
//        sheet.getLib().add(new Gustos3NumberIndicator(data));

        sheet.calcIndicators();

        new Visualizator(sheet);

    }


    static Instrument[] instruments = new Instrument[]{
            new Instrument("BTC","USDT"),
            new Instrument("ETH","USDT"),
            new Instrument("QTUM","USDT"),
            new Instrument("BCC","USDT"),
            new Instrument("EOS","BTC"),
            new Instrument("ONT","BTC"),
            new Instrument("XRP","BTC"),
            new Instrument("TRX","BTC"),
            new Instrument("ADA","BTC"),
            new Instrument("NEO","BTC"),
            new Instrument("QKC","BTC"),
            new Instrument("BNB","BTC"),
            new Instrument("LTC","BTC"),
            new Instrument("VEN","BTC"),
            new Instrument("ZIL","BTC"),
            new Instrument("GTO","BTC"),
            new Instrument("GNT","BTC"),
            new Instrument("DASH","BTC"),
            new Instrument("LOOM","BTC"),
            new Instrument("ETC","BTC"),
    };

    static Instances set;
    public static void main(String[] args) throws Exception {
        out("");
        out(ZonedDateTime.now().toString());
//        out("BNB:");
//        printTest(Instrument.Companion.getBNB_USDT(), "gustoslogic_bnb.properties");
//        out("BCC:");
//        printTest(Instrument.Companion.getBCC_USDT(), "gustoslogic_bcc.properties");
        out("BTC:");
//        String properties = "gustoslogic2_.properties";
//        String logic = "gustostest";
        String properties = "gustoslogic2_.properties";
        String logic = "gustos2";
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("1"));
        attributes.add(new Attribute("2"));
        attributes.add(new Attribute("3"));
        attributes.add(new Attribute("4"));
        attributes.add(new Attribute("5"));
        attributes.add(new Attribute("profit", Arrays.asList("false", "true")));
//        attributes.add(new Attribute("1b"));
//        attributes.add(new Attribute("2b"));
//        attributes.add(new Attribute("3b"));
//        attributes.add(new Attribute("4b"));
//        attributes.add(new Attribute("5b"));
        set = new Instances("data",attributes,10);
//        String properties = "levels.properties.out";
//        String logic = "levels";
        for (Instrument i : instruments)
            printTest(i, logic,properties);
        Exporter.string2file("d:/weka/plusminus.arff",set.toString());
//        printTest(Instrument.Companion.getBTC_USDT(), logic,properties);
//        out("BTCi:");
//        printTest(Instrument.Companion.getBTC_USDT(), "gustoslogic2i.properties");
//        sheet = TestUtils.makeSheet("indicators_simple.json");
//        show(sheet,logic, properties);

    }

}

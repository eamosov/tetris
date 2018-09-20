package ru.gustos.trading.global;

import org.apache.commons.io.FileUtils;
import ru.efreet.trading.bars.MarketBar;
import ru.efreet.trading.bars.MarketBarFactory;
import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.exchange.BarInterval;
import ru.efreet.trading.exchange.Exchange;
import ru.efreet.trading.exchange.Instrument;
import ru.efreet.trading.exchange.impl.Binance;
import ru.efreet.trading.exchange.impl.cache.BarsCache;
import ru.gustos.trading.global.timeseries.TimeSeriesDouble;

import java.io.*;
import java.time.ZonedDateTime;
import java.util.*;

public class ExperimentData {
    public ArrayList<String> instruments = new ArrayList<>();
    public ArrayList<InstrumentData> data = new ArrayList<>();

    public PLHistoryAnalyzer planalyzer1;
    public PLHistoryAnalyzer planalyzer2;
    public PLHistoryAnalyzer planalyzer3;


    public List<MarketBar> marketBars;
    ZonedDateTime from, to;
    private boolean withml;
    private boolean withbuysell;
    File folder;
    boolean fullSave;

    ArrayList<Model> models = new ArrayList<>();

    public ExperimentData(ZonedDateTime from, ZonedDateTime to, boolean fullSave, String folder) throws IOException {
        this.from = from;
        this.to = to;
        this.fullSave = fullSave;
        planalyzer1 = new PLHistoryAnalyzer(false);
        planalyzer2 = new PLHistoryAnalyzer(false);
        planalyzer3 = new PLHistoryAnalyzer(false);
        this.folder = new File(folder);
        if (fullSave) {
            FileUtils.deleteDirectory(this.folder);
            this.folder.mkdir();
        }

    }

    ExperimentData(ObjectInputStream in, boolean noMarketBars) throws IOException, ClassNotFoundException {
        from = (ZonedDateTime) in.readObject();
        to = (ZonedDateTime) in.readObject();
        boolean withMarket = in.readBoolean() && !noMarketBars;
        ArrayList<String> instr = (ArrayList<String>) in.readObject();
        if (withMarket)
            initMarketBars();
        loadInstruments(withMarket, instr.stream().map(Instrument.Companion::parse).toArray(Instrument[]::new), withml, withbuysell);
        planalyzer1 = new PLHistoryAnalyzer(in);
        planalyzer2 = new PLHistoryAnalyzer(in);
        planalyzer3 = new PLHistoryAnalyzer(in);
        planalyzer2.loadModelTimes(in);

    }

    public static ExperimentData loadGeneral(String file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            return new ExperimentData(in,true);
        }
    }

    public static ExperimentData load(String folderName) throws IOException, ClassNotFoundException {
        ExperimentData result = loadGeneral(folderName + File.separator + "general");
        File folder = new File(folderName);
        result.folder = folder;
        result.findModels();
        return result;
    }

    private void findModels() {
        String[] list = folder.list();
        for (String s : list)
            if (s.startsWith("model-"))
                models.add(new Model(s));
    }

    public void loadInstruments(boolean withMarketBars, Instrument[] instruments, boolean withml, boolean withbuysell) {
        this.withml = withml;
        this.withbuysell = withbuysell;
        if (withMarketBars)
            initMarketBars();
        for (Instrument ii : instruments)
            addInstrument(ii, withml, withbuysell);


    }

    public void loadData(String key) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(new File(folder,key)))) {
            getInstrument(key).loadData(in);
        }
    }

    public void save() throws Exception {
        if (fullSave) {
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(folder,"general")))) {
                saveGeneral(out);
            }
            for (int i = 0; i < data.size(); i++) {
                InstrumentData instr = data.get(i);
                try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(folder, instr.instrument.toString())))) {
                    instr.saveData(out);
                }
            }
        }
    }

    public void saveModel(String instrument, FilterMomentsModel model) throws IOException {
        if (fullSave && model.classifier!=null){
            Model m = new Model(instrument, model.index);
            models.add(m);
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(folder,m.filename())))) {
                out.writeObject(model.classifier);
                out.writeObject(model.attFilter);
            }
        }
    }



    public void saveGeneral(ObjectOutputStream out) throws IOException {
        out.writeObject(from);
        out.writeObject(to);
        out.writeBoolean(marketBars!=null);

        out.writeObject(instruments);
        planalyzer1.saveHistories(out);
        planalyzer2.saveHistories(out);
        planalyzer3.saveHistories(out);
        planalyzer2.saveModelTimes(out);
    }



    private int index(String instrument) {
        for (int i = 0; i < instruments.size(); i++)
            if (instruments.get(i).equalsIgnoreCase(instrument))
                return i;
        return -1;

    }

    public InstrumentData getInstrument(String key) {
        int index = index(key);
        if (index>=0)
            return data.get(index);
        throw new NullPointerException("instrument not found " + key);
    }

    public void setResult(String key, InstrumentData data) {
        int index = index(key);
        if (index<0)
            throw new NullPointerException("instrument not found " + key);
        this.data.set(index,data);
    }

    private int marketBarIndex(ZonedDateTime end) {
        for (int i = 0; i < marketBars.size(); i++)
            if (marketBars.get(i).getEndTime().isAfter(end)) return i - 1;
        throw new NullPointerException("no market bar for this end time");
    }

    public TimeSeriesDouble makeMarketAveragePrice(PLHistoryAnalyzer pl, TimeSeriesDouble trades, HashSet<String> ignore) {
        TimeSeriesDouble result = new TimeSeriesDouble(trades.size());
        double[] k = new double[pl.histories.size()];
        for (int j = 0; j < pl.histories.size(); j++) {
            PLHistory plHistory = pl.histories.get(j);
            k[j] = plHistory.profitHistory.size() == 0 ? 0 : 1.0 / plHistory.profitHistory.get(0).buyCost;
        }

        for (int i = 0; i < trades.size(); i++) {
            long time = trades.time(i);
            double sum = 0;
            int cc = 0;
            for (int j = 0; j < pl.histories.size(); j++)
                if (ignore == null || !ignore.contains(pl.histories.get(j).instrument)) {
                    XBar bar = getInstrument(pl.histories.get(j).instrument).getBarAt(time);
                    if (bar != null) {
                        sum += bar.getClosePrice() * k[j];
                        cc++;
                    }
                }
            if (cc > 0)
                sum /= cc;
            else
                sum = 1;
            result.add(sum, time);
        }
        return result;
    }


    private void addInstrument(Instrument ii, boolean withml, boolean withbuysell) {
        Exchange exch = new Binance();
        BarInterval interval = BarInterval.ONE_MIN;
        if (DecisionManager.LOGS)
            System.out.println("Loading instrument: " + ii.toString());
        BarsCache cache = new BarsCache("cache.sqlite3");
        ArrayList<? extends XBar> bars = new ArrayList<>(cache.getBars(exch.getName(), ii, interval, from, to));
        List<MarketBar> mb = null;
        if (marketBars != null)
            mb = syncBars(bars);

        instruments.add(ii.toString());
        this.data.add(new InstrumentData(exch, ii, bars, mb, withml, withbuysell));
    }

    ArrayList<MarketBar> syncBars(ArrayList<? extends XBar> bars) {
        int marketFrom = marketBarIndex(bars.get(0).getEndTime());
        ArrayList<MarketBar> result = new ArrayList<>(bars.size());
        int next = marketFrom + 1;
        for (int i = 0; i < bars.size(); i++) {
            while (next < marketBars.size() && bars.get(i).getEndTime().isAfter(marketBars.get(next).getEndTime()))
                next++;
            result.add(marketBars.get(next - 1));
        }
        return result;

    }


    public void initMarketBars() {
        if (DecisionManager.LOGS)
            System.out.println("Prepare market bars");
        BarsCache cache = new BarsCache("cache.sqlite3");
        MarketBarFactory market = new MarketBarFactory(cache, BarInterval.ONE_MIN, "binance");
        marketBars = market.build(from, to);
//        marketBars.add(0, marketBars.get(0)); // simulate time lag
    }

    static class Model implements Serializable{
        String instrument;
        int index;

        public Model(String filename) {
            String[] s = filename.split("-");
            instrument = s[1];
            index = Integer.parseInt(s[2]);
        }

        public Model(String instrument, int index) {
            this.instrument = instrument;
            this.index = index;
        }

        public String filename(){
            return "model-"+instrument+"-"+index;
        }
    }

}


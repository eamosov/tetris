package ru.gustos.trading.global;

import kotlin.Pair;
import ru.gustos.trading.book.indicators.EmaRecurrent;
import ru.gustos.trading.global.timeseries.TimeSeriesDouble;
import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.io.*;
import java.util.*;

public class PLHistoryAnalyzer {
    public ArrayList<PLHistory> histories = new ArrayList<>();

    Classifier model = null;
    ArrayList<Attribute> attributes = new ArrayList<>();
    Instances testset;
    public Instances trainset;
    boolean withModel;

    public PLHistoryAnalyzer(boolean withModel) {
        this.withModel = withModel;
        attributes.add(new Attribute("1"));
        attributes.add(new Attribute("2"));
        attributes.add(new Attribute("3"));
        attributes.add(new Attribute("4"));
        attributes.add(new Attribute("5"));
        attributes.add(new Attribute("profit", Arrays.asList("false", "true")));
        testset = new Instances("data", attributes, 10);
        testset.setClassIndex(attributes.size() - 1);

    }

    public PLHistoryAnalyzer(ObjectInputStream in) throws IOException {
        histories = loadHistories(in);
    }

    public void add(PLHistory h) {
        histories.add(h);
    }

    public PLHistory get(String instrument) {
        for (PLHistory h : histories)
            if (h.instrument.equals(instrument))
                return h;
        return null;
    }

    public void clearHistories() {
        for (int i = 0;i<histories.size();i++)
            histories.set(i,new PLHistory(histories.get(i).instrument,null));

    }

    void newHistoryEvent(PLHistory history) {

    }

    private boolean goodModel() {
        return trainset != null && trainset.size() > 200;
    }

    boolean shouldBuy(PLHistory history) {
        if (!withModel) return true;
        if (history.profitHistory.size() < attributes.size()) return false;
        if (!goodModel()) return false;
        DenseInstance ii = makeInstance(history, history.profitHistory.size() - 1);
        ii.setDataset(testset);
        try {
            return model.classifyInstance(ii) > 0.5;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void validateModel() {
        if (!withModel) return;
        trainset = new Instances("data", attributes, 10);
        for (int i = 0; i < histories.size(); i++) {
            PLHistory h = histories.get(i);
            addHistoryToSet(trainset, h);
        }
        trainset.setClassIndex(attributes.size() - 1);
        RandomForest rf = new RandomForest();
        rf.setMaxDepth(5);
        rf.setNumFeatures(2);
        rf.setNumIterations(500);
        model = rf;
//            System.out.println("prepare model "+trainset.size());
        if (!goodModel())
            return;

        try {
            rf.buildClassifier(trainset);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void addHistoryToSet(Instances set, PLHistory h) {
        int n = attributes.size() - 1;
        for (int j = n; j < h.profitHistory.size(); j++)
            set.add(makeInstance(h, j));
    }

    private DenseInstance makeInstance(PLHistory h, int index) {
        int n = attributes.size() - 1;
        double[] ii = new double[attributes.size()];
        for (int k = 0; k < n; k++)
            ii[k] = (h.profitHistory.get(index - n + k).profit - 1) * 100;
        double p = h.profitHistory.get(index).profit;
        ii[n] = p > 1 ? 1 : 0;
        double w = p > 1 ? p - 1 : 1 / p - 1;
        return new DenseInstance(w, ii);
    }

    public TimeSeriesDouble makeHistory(boolean onlyTested, double moneyPart, HashSet<String> ignore) {
        ArrayList<PLHistory.PLTrade> prepare = new ArrayList<>();
        for (PLHistory h : histories)
            if (ignore == null || !ignore.contains(h.instrument))
                for (int i = 0; i < h.profitHistory.size(); i++) {
                    PLHistory.PLTrade e = h.profitHistory.get(i);
                    if (!onlyTested || e.tested)
                        prepare.add(e);
                }
        prepare.sort(Comparator.comparingLong(c -> c.timeSell));
        TimeSeriesDouble result = new TimeSeriesDouble(prepare.size());
        double m = 1;
        for (int i = 0; i < prepare.size(); i++) {
            PLHistory.PLTrade p = prepare.get(i);
            m *= (p.profit - 1) * moneyPart + 1;
            result.add(m, p.timeSell);
        }
        return result;
    }

    public TimeSeriesDouble makeHistoryCorrect(boolean onlyTested, double moneyPart, long pause, HashSet<String> ignore) {
        ArrayList<PLHistory.PLTrade> prepare = new ArrayList<>();
        for (PLHistory h : histories)
            if (ignore == null || !ignore.contains(h.instrument))
                for (int i = 0; i < h.profitHistory.size(); i++) {
                    PLHistory.PLTrade e = h.profitHistory.get(i);
                    e.instrument = h.instrument;
                    if (!onlyTested || e.tested)
                        prepare.add(e);
                }
        prepare.sort(Comparator.comparingLong(c -> c.timeBuy));
        ArrayList<Pair<Double,PLHistory.PLTrade>> toSell = new ArrayList<>();

        TimeSeriesDouble result = new TimeSeriesDouble(prepare.size());
        double m = 1;
        double inactives = 0;
        long lastbuy = 0;
        Hashtable<String, EmaRecurrent> pows = new Hashtable<>();

        for (int i = 1; i < prepare.size()-1; i++) {
            PLHistory.PLTrade p = prepare.get(i);
            for (int j = toSell.size()-1;j>=0;j--){
                PLHistory.PLTrade sell = toSell.get(j).getSecond();
                if (sell.timeSell<=p.timeBuy){
                    m+=toSell.get(j).getFirst()*sell.profit;
                    inactives-=toSell.get(j).getFirst();
                    result.add(m+inactives, p.timeSell);

                    EmaRecurrent ema = pows.get(sell.instrument);
                    if (ema==null)
                        pows.put(sell.instrument,ema = new EmaRecurrent(10));
                    ema.feed(sell.profit);

                    toSell.remove(j);
                }
            }

            if (m<=0) {
//            if (m<=0 || (near(p.timeBuy,prepare.get(i+1).timeBuy) && !near(p.timeBuy,prepare.get(i-1).timeBuy))) {
//            if (m<=0 || ((near(p.timeBuy,prepare.get(i+1).timeBuy) || near(p.timeBuy,prepare.get(i-1).timeBuy)) && !p.instrument.equals(bestOfSimulataneous(pows,prepare,i)))) {
//            if (m<=0 || (near(p.timeBuy,prepare.get(i+1).timeBuy) || near(p.timeBuy,prepare.get(i-1).timeBuy))) {
                if (m>0) {
//                    int j = i;
//                    while (j<prepare.size() && near(p.timeBuy,prepare.get(j).timeBuy)){
//                        System.out.print(prepare.get(j).instrument+"("+prepare.get(j).profit+") ");
//                        j++;
//                    }
                    System.out.println("skip "+p.instrument+" "+p.profit);
                }
//            if (m<=0 || !near(p.timeBuy,prepare.get(i-1).timeBuy) ) {
//            if (m<=0 || p.timeBuy-lastbuy<pause) {
//                lastbuy = p.timeBuy;
                PLHistory.PLTrade t = prepare.get(i);
                EmaRecurrent ema = pows.get(t.instrument);
                if (ema==null)
                    pows.put(t.instrument,ema = new EmaRecurrent(10));
                ema.feed(t.profit);

                continue;
            }
            double part = Math.min(m,moneyPart*(m+inactives));
            m-=part;
            inactives+=part;
            lastbuy = p.timeBuy;
            toSell.add(new Pair<>(part,p));
        }
        return result;
    }

    private String bestOfSimulataneous(Hashtable<String, EmaRecurrent> pows, ArrayList<PLHistory.PLTrade> prepare, int pos) {
        int from = pos;
        while (from>0 && near(prepare.get(from).timeBuy,prepare.get(from-1).timeBuy))
            from--;
        int to = pos;
        while (to<prepare.size()-1 && near(prepare.get(to).timeBuy,prepare.get(to+1).timeBuy))
            to++;

        String best = null;
        double bestprofit = 1.001;
        for (int i = from;i<=to;i++){
            PLHistory.PLTrade t = prepare.get(i);
            EmaRecurrent ema = pows.get(t.instrument);
            if (ema!=null && !t.instrument.equals("ETH_USDT") && !t.instrument.equals("LTC_USDT")){
                if (ema.value()>bestprofit) {
                    bestprofit = ema.value();
                    best = t.instrument;
                }

            }
        }
        return best;
//        return prepare.get(from+1).instrument;
    }

    private boolean near(long t1, long t2) {
        return Math.abs(t1-t2)<1;
    }

    public ArrayList<Long> makeModelTimes(HashSet<String> ignore) {
        ArrayList<Long> prepare = new ArrayList<>();
        for (PLHistory h : histories)
            if (ignore == null || !ignore.contains(h.instrument))
                prepare.addAll(h.modelTimes);

        prepare.sort(Comparator.naturalOrder());
        return prepare;
    }

    public TimeSeriesDouble makeHistory(String instrument) {
        for (PLHistory h : histories)
            if (h.instrument.equalsIgnoreCase(instrument)) {
                ArrayList<PLHistory.PLTrade> prepare = new ArrayList<>();
                for (int i = 0; i < h.profitHistory.size(); i++) {
                    PLHistory.PLTrade e = h.profitHistory.get(i);
                    prepare.add(e);
                }
                prepare.sort(Comparator.comparingLong(c -> c.timeSell));
                TimeSeriesDouble result = new TimeSeriesDouble(prepare.size());
                double m = 1;
                for (int i = 0; i < prepare.size(); i++) {
                    PLHistory.PLTrade p = prepare.get(i);
                    m *= p.profit;
                    result.add(m, p.timeSell);
                }
                return result;
            }
        return null;
    }

    public String profits() {
        StringBuilder sb = new StringBuilder();
        int cc = 0;
        int cp = 0;
        int lower1 = 0,lower2 = 0,lower3 = 0, lower4 = 0;
        int higher1 = 0,higher2 = 0,higher3 = 0;
        double m = 1;
        for (PLHistory h : histories) {
            if (sb.length() > 0)
                sb.append(",");
            sb.append(String.format("%s:%.4g*%.2g(%d of %d)", h.instrument, h.all.profit, h.all.drawdown, h.all.goodcount, h.all.count));
            cc += h.all.count;
            cp += h.all.goodcount;
            m*=h.all.profit;
            lower1+=h.countSLBreaking(0.01);
            lower2+=h.countSLBreaking(0.02);
            lower3+=h.countSLBreaking(0.03);
            lower4+=h.countSLBreaking(0.05);
            higher1+=h.countTPBreaking(0.01);
            higher2+=h.countTPBreaking(0.02);
            higher3+=h.countTPBreaking(0.03);
        }
//        sb.append(", trades ").append(cp).append(" of ").append(cc);
        sb.append(String.format(", SL 1%% %d%%, 2%% %d%%, 3%% %d%%, 5%% %d%%, TP 1%% %d%%, 2%% %d%%, 3%% %d%%", lower1*100/cc,lower2*100/cc,lower3*100/cc,lower4*100/cc, higher1*100/cc,higher2*100/cc,higher3*100/cc));

        return String.format("total: %.4g (%d of %d)", m,cp,cc)+sb.toString();
    }


    public TimeSeriesDouble makeHistoryNormalized(boolean onlyTested, double moneyPart, TimeSeriesDouble normTo, HashSet<String> ignore) {
        ArrayList<PLHistory.PLTrade> prepare = new ArrayList<>();
        for (PLHistory h : histories)
            if (ignore == null || !ignore.contains(h.instrument))
                for (int i = 0; i < h.profitHistory.size(); i++) {
                    PLHistory.PLTrade e = h.profitHistory.get(i);
                    if (!onlyTested || e.tested)
                        prepare.add(e);
                }
        prepare.sort(Comparator.comparingLong(c -> c.timeSell));
        TimeSeriesDouble result = new TimeSeriesDouble(prepare.size());
        int normToIndex = 0;
        double m = 1;
        for (int i = 0; i < prepare.size(); i++) {
            PLHistory.PLTrade p = prepare.get(i);
            m *= (p.profit - 1) * moneyPart + 1;
            while (normToIndex < normTo.size() - 1 && normTo.time(normToIndex + 1) <= p.timeSell) normToIndex++;
            result.add(m / normTo.get(normToIndex), p.timeSell);
        }
        return result;
    }


    public void saveHistories(ObjectOutputStream out) throws IOException {
        out.writeInt(histories.size());
        for (int i = 0; i < histories.size(); i++)
            histories.get(i).save(out);
    }

    public static ArrayList<PLHistory> loadHistories(ObjectInputStream in) throws IOException {
        int cc = in.readInt();
        ArrayList<PLHistory> res = new ArrayList<>(cc);
        for (int i = 0; i < cc; i++) {
            res.add(new PLHistory(in));
        }
        return res;
    }

    public void saveModelTimes(ObjectOutputStream out) throws IOException {
        for (int i = 0; i < histories.size(); i++) {
            ArrayList<Long> times = histories.get(i).modelTimes;
            out.writeInt(times.size());
            for (int j = 0; j < times.size(); j++)
                out.writeLong(times.get(j));
        }
    }

    public void loadModelTimes(ObjectInputStream in) throws IOException {
        for (int i = 0; i < histories.size(); i++) {
            int cc = in.readInt();
            while (cc-- > 0)
                histories.get(i).modelTimes.add(in.readLong());
        }
    }

}

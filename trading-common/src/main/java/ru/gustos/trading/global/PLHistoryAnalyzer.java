package ru.gustos.trading.global;

import kotlin.Pair;
import ru.gustos.trading.global.timeseries.TimeSeriesDouble;
import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

public class PLHistoryAnalyzer{
    public ArrayList<PLHistory> histories = new ArrayList<>();

    Classifier model = null;
    ArrayList<Attribute> attributes = new ArrayList<>();
    Instances testset;
    public Instances trainset;
    boolean withModel;

    public PLHistoryAnalyzer(boolean withModel){
        this.withModel = withModel;
        attributes.add(new Attribute("1"));
        attributes.add(new Attribute("2"));
        attributes.add(new Attribute("3"));
        attributes.add(new Attribute("4"));
        attributes.add(new Attribute("5"));
        attributes.add(new Attribute("profit", Arrays.asList("false", "true")));
        testset = new Instances("data",attributes,10);
        testset.setClassIndex(attributes.size()-1);

    }
    public PLHistoryAnalyzer(DataInputStream in) throws IOException {
        histories = loadHistories(in);
    }

    public void add(PLHistory h){
        histories.add(h);
    }

    public PLHistory get(String instrument){
        for (PLHistory h : histories)
            if (h.instrument.equals(instrument))
                return h;
        return null;
    }

    void newHistoryEvent(PLHistory history) {

    }

    private boolean goodModel(){
        return trainset!=null && trainset.size()>200;
    }

    boolean shouldBuy(PLHistory history){
        if (!withModel) return true;
        if (history.profitHistory.size()<attributes.size()) return false;
        if (!goodModel()) return false;
        DenseInstance ii = makeInstance(history, history.profitHistory.size() - 1);
        ii.setDataset(testset);
        try {
            return model.classifyInstance(ii)>0.5;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void validateModel() {
        if (!withModel) return;
        trainset = new Instances("data",attributes,10);
        for (int i = 0;i<histories.size();i++){
            PLHistory h = histories.get(i);
            addHistoryToSet(trainset,h);
        }
        trainset.setClassIndex(attributes.size()-1);
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
        for (int j = n;j<h.profitHistory.size();j++)
            set.add(makeInstance(h,j));
    }

    private DenseInstance makeInstance(PLHistory h, int index){
        int n = attributes.size() - 1;
        double[] ii = new double[attributes.size()];
        for (int k = 0; k< n; k++)
            ii[k] = (h.profitHistory.get(index-n+k).profit-1)*100;
        double p = h.profitHistory.get(index).profit;
        ii[n] = p >1?1:0;
        double w = p>1?p-1:1/p-1;
        return new DenseInstance(w, ii);
    }

    public TimeSeriesDouble makeHistory(boolean onlyTested, double moneyPart, HashSet<String> ignore) {
        ArrayList<PLHistory.PLTrade> prepare = new ArrayList<>();
        for (PLHistory h : histories) if (ignore==null || !ignore.contains(h.instrument))
            for (int i = 0;i<h.profitHistory.size();i++) {
                PLHistory.PLTrade e = h.profitHistory.get(i);
                if (!onlyTested || e.tested)
                    prepare.add(e);
            }
        prepare.sort(Comparator.comparingLong(c -> c.timeSell));
        TimeSeriesDouble result = new TimeSeriesDouble(prepare.size());
        double m = 1;
        for (int i = 0;i<prepare.size();i++){
            PLHistory.PLTrade p = prepare.get(i);
            m*=(p.profit-1)*moneyPart+1;
            result.add(m,p.timeSell);
        }
        return result;
    }

    public ArrayList<Long> makeModelTimes(HashSet<String> ignore) {
        ArrayList<Long> prepare = new ArrayList<>();
        for (PLHistory h : histories) if (ignore==null || !ignore.contains(h.instrument))
            prepare.addAll(h.modelTimes);

        prepare.sort(Comparator.naturalOrder());
        return prepare;
    }

    public TimeSeriesDouble makeHistory(String instrument){
        for (PLHistory h : histories) if (h.instrument.equalsIgnoreCase(instrument)){
            ArrayList<PLHistory.PLTrade> prepare = new ArrayList<>();
            for (int i = 0;i<h.profitHistory.size();i++) {
                PLHistory.PLTrade e = h.profitHistory.get(i);
                prepare.add(e);
            }
            prepare.sort(Comparator.comparingLong(c -> c.timeSell));
            TimeSeriesDouble result = new TimeSeriesDouble(prepare.size());
            double m = 1;
            for (int i = 0;i<prepare.size();i++){
                PLHistory.PLTrade p = prepare.get(i);
                m*=p.profit;
                result.add(m,p.timeSell);
            }
            return result;
        }
        return null;
    }

    public String profits(){
        StringBuilder sb = new StringBuilder();
        int cc = 0;
        int cp = 0;
        for (PLHistory h : histories) {
            if (sb.length()>0)
                sb.append(",");
            sb.append(String.format("%s:%.4g*%.2g(%d of %d)",h.instrument,h.all.profit,h.all.drawdown,h.all.goodcount,h.all.count));
            cc+=h.all.count;
            cp+=h.all.goodcount;
        }
        sb.append(", trades ").append(cp).append(" of ").append(cc);
        return sb.toString();
    }


    public TimeSeriesDouble makeHistoryNormalized(boolean onlyTested, double moneyPart, TimeSeriesDouble normTo, HashSet<String> ignore) {
        ArrayList<PLHistory.PLTrade> prepare = new ArrayList<>();
        for (PLHistory h : histories)  if (ignore==null || !ignore.contains(h.instrument))
            for (int i = 0;i<h.profitHistory.size();i++) {
                PLHistory.PLTrade e = h.profitHistory.get(i);
                if (!onlyTested || e.tested)
                    prepare.add(e);
            }
        prepare.sort(Comparator.comparingLong(c -> c.timeSell));
        TimeSeriesDouble result = new TimeSeriesDouble(prepare.size());
        int normToIndex = 0;
        double m = 1;
        for (int i = 0;i<prepare.size();i++){
            PLHistory.PLTrade p = prepare.get(i);
            m*=(p.profit-1)*moneyPart+1;
            while (normToIndex<normTo.size()-1 && normTo.time(normToIndex+1)<=p.timeSell) normToIndex++;
            result.add(m/normTo.get(normToIndex),p.timeSell);
        }
        return result;
    }


    public void saveHistories(DataOutputStream out) throws IOException {
        out.writeInt(histories.size());
        for (int i = 0;i<histories.size();i++)
            histories.get(i).save(out);
    }

    public static ArrayList<PLHistory> loadHistories(DataInputStream in) throws IOException {
        int cc = in.readInt();
        ArrayList<PLHistory> res = new ArrayList<>(cc);
        for (int i = 0;i<cc;i++){
            res.add(new PLHistory(in));
        }
        return res;
    }

    public void saveModelTimes(DataOutputStream out) throws IOException {
        for (int i = 0;i<histories.size();i++) {
            ArrayList<Long> times = histories.get(i).modelTimes;
            out.writeInt(times.size());
            for (int j = 0;j<times.size();j++)
                out.writeLong(times.get(j));
        }
    }

    public void loadModelTimes(DataInputStream in) throws IOException {
        for (int i = 0;i<histories.size();i++) {
            int cc = in.readInt();
            while (cc-->0)
                histories.get(i).modelTimes.add(in.readLong());
        }
    }
}

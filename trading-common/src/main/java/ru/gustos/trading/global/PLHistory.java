package ru.gustos.trading.global;

import kotlin.Pair;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

public class PLHistory {

    public String instrument;
    double buyCost = 0;
    boolean testedBuy;
    long timeBuy;

    public ArrayList<PLTrade> profitHistory = new ArrayList<>();
    public Stat tested = new Stat();
    public Stat all = new Stat();

    PLHistoryAnalyzer analyzer;
    ArrayList<Long> modelTimes = new ArrayList<>();

    public PLHistory(DataInputStream in) throws IOException {
        load(in);
    }

    public PLHistory(String instrument, PLHistoryAnalyzer analyzer){
        this.instrument = instrument;
        this.analyzer = analyzer;
        if (analyzer!=null)
            analyzer.add(this);
    }

    public PLHistory(PLHistory src, long from, long to){
        instrument = src.instrument;
        for (PLTrade t : src.profitHistory)
            if (t.timeBuy>=from && t.timeSell<to) {
                profitHistory.add(t);
                all.add(t.profit);
            }

    }

    public boolean buyMoment(double cost, long time){
        if (buyCost==0) {
            buyCost = cost;
            testedBuy = shouldBuy();
            timeBuy = time;
            return true;
        }
        return false;
    }

    public boolean sellMoment(double cost, long timeSell){
        if (buyCost!=0) {
            double profit = cost * 0.998 / buyCost;
            if (testedBuy)
                tested.add(profit);
            all.add(profit);
            profitHistory.add(new PLTrade(buyCost,profit,timeBuy, timeSell,testedBuy));
            buyCost = 0;
            if (analyzer!=null)
                analyzer.newHistoryEvent(this);
            return true;
        }
        return false;
    }

    public String toPlusMinusString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0;i<profitHistory.size();i++)
            sb.append(profitHistory.get(i).profit>1?"+":"-");
        return sb.toString();
    }

    public PLTrade lastTrade(int fromEnd) {
        int index = profitHistory.size() - 1 - fromEnd;
        if (index<0) return null;
        return profitHistory.get(index);
    }

    public double lastProfit(int fromEnd){
        PLTrade p = lastTrade(fromEnd);
        if (p==null) return 1;
        return p.profit;
    }

    public double totalProfit(){
        double m = 1;
        for (PLTrade t : profitHistory)
            m*=t.profit;
        return m;
    }

    public double dropdown(){
        double m = 1;
        for (PLTrade t : profitHistory)
            m*=t.profit;
        return m;
    }

    public int size(){
        return profitHistory.size();
    }

    public double getPossibleProfit(long time) {
        if (size()==0) return 0;
        if (size()<=3){
            double min = profitHistory.stream().mapToDouble(p->p.profit).min().getAsDouble();
            double avg = profitHistory.stream().mapToDouble(p->p.profit).sum()/size();
            return min+(avg-min)*0.5;
        }
        SimpleRegression r = new SimpleRegression();
        double money = 1;
        for (int i = 0;i<4;i++) {
            PLTrade p = profitHistory.get(size()-1-i);
            r.addData(p.timeBuy - time, money);
            money /= p.profit;
        }
        return r.predict(0)-r.getInterceptStdErr()/2;
    }



    public boolean shouldBuy(){
        return shouldBuy(profitHistory.size());
    }

    public boolean simpleTest(){
        return simpleTest(profitHistory.size());
    }

    private boolean simpleTest(int pos){
        if (pos<3) return false;
        double limit = 1;
        return (profitHistory.get(pos-1).profit< limit?1:0) + (profitHistory.get(pos-2).profit< limit?1:0) + (profitHistory.get(pos-3).profit< limit?1:0)<=1;
    }

    public static int needGoodHistory = 65;
    public boolean shouldBuy(int pos){
        if (pos<10) return false;
        int i = pos;
        double limit = 1;
        if ((profitHistory.get(i-1).profit< limit?1:0) + (profitHistory.get(i-2).profit< limit?1:0) + (profitHistory.get(i-3).profit< limit?1:0)>1) return false;

        double m = 1;
        int cc = 0;
        i--;
        do {

            PLTrade t = profitHistory.get(i);
            if (simpleTest(i)) {
                m *= t.profit;
                cc++;
            }
            i--;
        } while (i>=0 && cc< needGoodHistory);
        if (m<1) return false;

        return true;
    }

    public ArrayList<CriticalMoment> getCriticalBuyMoments(double limit, boolean good, boolean bad) {
        ArrayList<CriticalMoment> times = new ArrayList<>();
        int goodc = 0, badc = 0;
        for (PLTrade t : profitHistory){
            if (good && t.profit>1+limit) {
                times.add(t.criticalMoment());
                goodc++;
            }if (bad && t.profit<1-limit) {
                times.add(t.criticalMoment());
                badc++;
            }
        }
//        System.out.println(goodc+" "+badc);
        return times;
    }


    public ArrayList<Long> getMostCriticalBuyMoments(boolean good, boolean bad, boolean buyTime, long backtime, long interval, int cnt) {
        if (profitHistory.size()==0) return new ArrayList<>();
        ArrayList<Pair<Long,Double>> times = new ArrayList<>();
        int goodc = 0, badc = 0;
        long from = profitHistory.get(profitHistory.size()-1).timeBuy-backtime-interval;
        for (PLTrade t : profitHistory) if (t.timeBuy>=from && t.timeBuy<from+interval){
            if (good && t.profit>1) {
                times.add(new Pair<>(buyTime?t.timeBuy:t.timeSell, 1-t.profit));
                goodc++;
            }
            if (bad && t.profit<1) {
                times.add(new Pair<>(buyTime?t.timeBuy:t.timeSell,t.profit));
                badc++;
            }
        }
        times.sort(Comparator.comparing(Pair::getSecond));
//        System.out.println(goodc+" "+badc);
        return times.stream().limit(cnt).map(Pair<Long, Double>::getFirst).collect(Collectors.toCollection(ArrayList::new));
    }




    @Override
    public String toString() {
        return String.format("tested: %s, all: %s", tested,all);
    }

    public void save(DataOutputStream out) throws IOException {
        out.writeUTF(instrument);
        tested.save(out);
        all.save(out);
        out.writeInt(profitHistory.size());
        for (int i = 0;i<profitHistory.size();i++){
            profitHistory.get(i).save(out);
        }
    }

    public void load(DataInputStream in) throws IOException {
        instrument = in.readUTF();
        tested.load(in);
        all.load(in);
        int cc = in.readInt();
        for (int i = 0;i<cc;i++){
            profitHistory.add(new PLTrade(in));
        }
    }

    public PLTrade findByBuyTime(long time) {
        for (PLTrade t : profitHistory)
            if (t.timeBuy == time)
                return t;
        return null;
    }

    public void newModel(ZonedDateTime time) {
        modelTimes.add(time.toEpochSecond());
    }

    public static class Stat {
        public double profit = 1;
        public double good = 1;
        public double bad = 1;
        public double drawdown = 1;
        public double max = 1;
        public int count = 0;
        public int goodcount = 0;

        void add(double p){
            count++;
            profit*=p;
            if (p>1) {
                good *= p;
                goodcount++;
            }else
                bad*=p;
            if (profit>max)
                max = profit;
            drawdown = Math.min(drawdown,profit/max);
        }

        @Override
        public String toString() {
            return String.format("profit %.3g, good %.3g, bad %.3g, drawdown %.3g, pertrade %.3g%%(*%d)", profit,good,bad, drawdown, (Math.pow(profit,1.0/count)-1)*100,count);
        }

        void save(DataOutputStream out) throws IOException {
            out.writeDouble(profit);
            out.writeDouble(good);
            out.writeDouble(bad);
            out.writeDouble(drawdown);
            out.writeDouble(max);
            out.writeInt(count);
        }

        public void load(DataInputStream in) throws IOException {
            profit = in.readDouble();
            good = in.readDouble();
            bad = in.readDouble();
            drawdown = in.readDouble();
            max = in.readDouble();
            count = in.readInt();
        }
    }

    public class PLTrade{
        public double buyCost;
        public double profit;
        public long timeBuy;
        public long timeSell;
        public boolean tested;

        public PLTrade(double buyCost, double profit, long timeBuy, long timeSell, boolean testedBuy) {
            this.buyCost = buyCost;
            this.profit = profit;
            this.timeBuy = timeBuy;
            this.timeSell = timeSell;
            this.tested = testedBuy;
        }

        public PLTrade(DataInputStream in) throws IOException {
            buyCost = in.readDouble();
            profit = in.readDouble();
            timeBuy = in.readLong();
            timeSell = in.readLong();
            tested = in.readBoolean();
        }

        void save(DataOutputStream out) throws IOException {
            out.writeDouble(buyCost);
            out.writeDouble(profit);
            out.writeLong(timeBuy);
            out.writeLong(timeSell);
            out.writeBoolean(tested);
        }

        public CriticalMoment criticalMoment(){
            return new CriticalMoment(timeBuy, timeSell, profit);
        }
    }

    public static class CriticalMoment {
        public long timeBuy;
        public long timeSell;
        public double profit;

        public CriticalMoment(long timeBuy, long timeSell, double profit){
            this.timeBuy = timeBuy;
            this.timeSell = timeSell;
            this.profit = profit;
        }

        public long time(boolean buy) {
            return buy?timeBuy:timeSell;
        }
    }
}



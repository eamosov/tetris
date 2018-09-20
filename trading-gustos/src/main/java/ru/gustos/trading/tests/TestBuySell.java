package ru.gustos.trading.tests;

import com.google.common.collect.Sets;
import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.exchange.Instrument;
import ru.gustos.trading.global.*;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;

import static ru.gustos.trading.tests.TestGlobal.instruments;

public class TestBuySell {

    static PLHistory total;
    static int days = 1;
    static HashSet<String> ignoreBuy = Sets.newHashSet();//,"sd_lag1","sd_delta1","sd_lag2","sd_delta2","macd0","macd1","macd2","macd3");
    

    public static void main(String[] args) throws Exception {
        for (Instrument instr : instruments) {
            System.out.println();
            System.out.println();
            System.out.println(instr.toString());
//            Global global = TestGlobal.init(new Instrument[]{instr}, true, true);
//            InstrumentData data = global.getInstrument(instr.toString());

//        DecisionManager c = new DecisionManager(null, data, 6, false, 0);
            total = new PLHistory(instr.toString(), null);
            for (int i = 0; i < 100; i +=days) {
                ExperimentData experimentData = TestGlobal.init(new Instrument[]{instr}, true, true,true);
                InstrumentData data = experimentData.getInstrument(instr.toString());

                DecisionManager c = new DecisionManager(null, null, data, 6, false, 0);
                tryGustosBranches(c, i);
            }
            System.out.println("result " + instr.toString() + ": " + total.all);
            System.out.println("profit with sl 1:" + total.profitWithStoploss(0.01));
            System.out.println("profit with sl 2:" + total.profitWithStoploss(0.02));
            System.out.println("profit with sl 3:" + total.profitWithStoploss(0.03));
            System.out.println("profit with sl 4:" + total.profitWithStoploss(0.04));
            System.out.println("profit with sl 5:" + total.profitWithStoploss(0.05));
            System.out.println("profit with tp 0.3:" + total.profitWithTakeprofit(0.003));
            System.out.println("profit with tp 0.5:" + total.profitWithTakeprofit(0.005));
            System.out.println("profit with tp 0.75:" + total.profitWithTakeprofit(0.0075));
            System.out.println("profit with tp 1:" + total.profitWithTakeprofit(0.01));
            System.out.println("profit with tp 1.25:" + total.profitWithTakeprofit(0.0125));
            System.out.println("profit with tp 1.5:" + total.profitWithTakeprofit(0.015));
        }

//        fillBuySell(data, true);
//        makeSteps(data,0);

//        makeTestTrees(data, 0);
//        System.out.println("\n\nsell");
//        fillBuySell(data, false);
//        makeTestTrees(data, 1);


//        TestGlobal.saveResults(global);

    }

    private static double tryGustosBranches(DecisionManager c, int fromday) throws Exception {
//        int from = DecisionManager.calcAllFrom + 24 * 60 * (fromday+60);
//        int to = from + 24 * 60 * 90;
        int validPeriod = 24 * 60 * 5;
        int learnPeriod = 90;
        int from = DecisionManager.calcAllFrom + 24 * 60 * (fromday-learnPeriod+150);
        int to = from + 24 * 60 * learnPeriod;
//        from+=24 * 60 *60;

        int examFrom = to+1;
        int validFrom = to - validPeriod;
        if (c.data.size() < examFrom + 24 * 60 * days) return 1;
        long time = c.data.bar(to).getEndTime().toEpochSecond();
        System.out.println(c.data.bar(to).getEndTime());
        Instances train = null;
        GustosBranches buy = null, sell = null;
        double examres = 0;

//        for (int i = 0; i < 5; i++) {
//            System.out.println(i+")");
        train = c.data.buyhelper.makeSet(c.data.buydata(), ignoreBuy, null, from, to /*- validPeriod*/, time, 0, 9, 0);
//        System.out.println("before: " + test(c, from, train.size()).all.toString());
        buy = new GustosBranches();
//        buy.build(train, 10, 100);
        buy.buildSelectingAttributes(train, 3,4, 100);
//        buy.limit = bestLimitBuy(c, train, from, buy,10000);


//        fill(c, buy, "gustosBuy");

//        System.out.println("after buy: " + test(c, from, train.size()).all.toString());
//        c.calc.prepareGoodSell();
//        train = c.data.helper.makeSet(c.data.data(), ignoreBuy, from, to, time, 1, 9, 0);
//        sell = new GustosBranches();
//        sell.build(train, 10, 100);
//        sell.limit = bestLimitSell(c, train, from, sell);
//
//        fill(c, sell, "gustosSell");
////
//        System.out.println("after sell: " + test(c, from, train.size()).all.toString());
//        c.calc.prepareGoodBuy();


        //      System.out.println("train: " + test(c, buy, sell, train, from).all.toString());
        Instances valid = c.data.buyhelper.makeSet(c.data.buydata(), ignoreBuy, null, validFrom, to, Long.MAX_VALUE, 0, 9, 0);
        Instances exam = c.data.buyhelper.makeSet(c.data.buydata(), ignoreBuy, null, examFrom, to + 24 * 60*days, Long.MAX_VALUE, 0, 9, 0);
        buy.printBranchStats("valid ",valid);
        buy.printBranchStats("exam ",exam);
        examres = exam(c, buy, null, valid, validFrom, exam, examFrom);
//        }
        return examres;
    }

    private static double exam(DecisionManager c, GustosBranches buy, GustosBranches sell, Instances valid, int validFrom, Instances exam, int examFrom) {
        int wasb = buy.limit;
//        int wass = sell.limit;
//        bestLimit(c, valid, validFrom, buy, sell);

        int limit = bestLimitBuy(c, valid, validFrom, buy, 10,2);
        buy.limit = limit;
        System.out.println("possibilities:");
        bestLimitBuy(c, exam, examFrom, buy, 10,1);
        if (limit == 1) return 1;


        fill(c, buy, c.data.buys);
        c.calc.prepareGoodBuy();
//        PLHistory h = test(c, examFrom, exam.size());
        PLHistory h = test(c, examFrom, exam.size(), total);
        System.out.println("EXAM: " + h.all.toString());
        System.out.println("total: "+total.all.toString());
//        PLHistory h = test(c, examFrom, exam.size());
//        testWithCorrection(c,buy,validFrom, valid.size(),null);
//        PLHistory h = testWithCorrection(c, buy, examFrom, exam.size(), total);
        buy.limit = wasb;
//        sell.limit = wass;
        return h.all.profit;
//        return 1;

    }

    private static PLHistory testWithCorrection(DecisionManager c, GustosBranches buy, int from, int count, PLHistory total) {
        PLHistory h = new PLHistory(c.data.instrument.toString(), null);
        ArrayList<Integer> buys = new ArrayList<>();
        for (int j = 0; j < count; j++) {
            int index = from + j;
            XBar bar = c.data.bars.get(index);
            double close = bar.getClosePrice();
            long time = bar.getEndTime().toEpochSecond();
            if (c.data.sells.get(index)) {
                if (total != null)
                    total.sellMoment(close, time);
                h.sellMoment(close, time);
                if (buys.size() > 0) {
                    correctBranches(c, buy, buys, index);
                    buys.clear();
                }
            } else if (checkBuy(c, buy, index)) {
                if (total != null)
                    total.buyMoment(close, time);
                h.buyMoment(close, time);
                buys.add(index);
            } else if (total != null) {
                total.minCost(bar.getMinPrice(), time);
                total.maxCost(bar.getMaxPrice(), time);
            }
        }
        return h;
    }

    private static void correctBranches(DecisionManager c, GustosBranches buy, ArrayList<Integer> buys, int sell) {
        double sellCost = c.data.bars.get(sell).getClosePrice();
        for (int i = 0; i < buys.size(); i++) {
            int ind = buys.get(i);
            double buyCost = c.data.bars.get(ind).getClosePrice();
            if (buyCost > sellCost) {
                Instance inst = c.data.buyhelper.makeInstance(c.data.buydata.get(ind), ignoreBuy, null,0, 9);
                inst.setDataset(c.data.buyhelper.makeEmptySet(ignoreBuy, null, 0, 9));
                buy.correctBad(inst);
            }
        }


    }

    private static PLHistory test(DecisionManager c, int from, int count, PLHistory total) {
        PLHistory h = new PLHistory(c.data.instrument.toString(), null);
        int goodBuy = 0, gustosBuy = 0, goodGustosBuy = 0, gustosSell = 0;
        StringBuilder buyIndexes =  new StringBuilder();
        for (int j = 0; j < count; j++) {
            int index = from + j;
            XBar bar = c.data.bars.get(index);
            double close = bar.getClosePrice();
            long time = bar.getEndTime().toEpochSecond();
            boolean good = c.data.helper.get(c.data.data.get(index), "_goodBuy") > 0 && c.data.data.get(index).weight > 0.3;
            if (good)
                goodBuy ++;
            if (c.data.sells.get(index)) {
                gustosSell++;
                h.sellMoment(close, time);
                if (total!=null)
                    total.sellMoment(close, time);
            }else if (c.data.buys.get(index)) {
                gustosBuy++;
                if (good)
                    goodGustosBuy++;
                if (h.buyMoment(close, time))
                    buyIndexes.append(" ").append(j).append(good?"+":"-");
                if (total!=null)
                    total.buyMoment(close, time);
            } else if (total!=null) {
                total.minCost(bar.getMinPrice(), time);
                total.maxCost(bar.getMaxPrice(), time);
            }
        }
        System.out.println(String.format("goodbuys: %.3g%%, gustosbuys: %.3g%%, good gustosbuys: %.3g%%, gustosSells: %.3g%% %s", goodBuy*100.0/count, gustosBuy*100.0/count, goodGustosBuy*100.0/count,gustosSell*100.0/count,buyIndexes.toString()));
        return h;
    }

    private static boolean checkBuy(DecisionManager c, GustosBranches br, int index) {
        Instance inst = c.data.buyhelper.makeInstance(c.data.buydata.get(index), ignoreBuy, null, 0, 9);
        inst.setDataset(c.data.buyhelper.makeEmptySet(ignoreBuy, null, 0, 9));
        return br.check(inst);
    }

    private static void fill(DecisionManager c, GustosBranches br, BitSet bits) {
        for (int i = 0; i < c.data.size(); i++)
            bits.set(i,checkBuy(c, br, i));

    }

    private static int bestLimitBuy(DecisionManager c, Instances set, int from, GustosBranches buy, int maxtrades, int fromCount) {
        int best = 1;
        double profit = 1;
        System.out.println("find limit for buy:");
        for (int i = Math.max(1,fromCount); i < 200; i++) {
            PLHistory h = new PLHistory(c.data.instrument.toString(), null);
            for (int j = 0; j < set.size(); j++) {
                int index = from + j;
                XBar bar = c.data.bars.get(index);
                if (c.data.sells.get(index)) {
                    h.sellMoment(bar.getClosePrice(), bar.getEndTime().toEpochSecond());
                } else if (buy.check(set.get(j), 0, i, 1))
                    h.buyMoment(bar.getClosePrice(), bar.getEndTime().toEpochSecond());

            }
            System.out.println(i + ")" + h.all);
            if (h.all.profit > profit && h.all.count <= maxtrades) {
                profit = h.all.profit;
                best = i;
            }
            if (h.all.count == 0) break;
        }
        System.out.println(String.format("best: %d (profit %.4g)", best, profit));
        return best<4?30:best;
    }

    private static int bestLimitSell(DecisionManager c, Instances train, int from, GustosBranches sell) {
        int best = 1;
        double profit = 0;
        System.out.println("find limit for sell:");
        for (int i = 2; i < 200; i++) {
            PLHistory h = new PLHistory(c.data.instrument.toString(), null);
            for (int j = 0; j < train.size(); j++) {
                int index = from + j;
                XBar bar = c.data.bars.get(index);
                if (c.data.buys.get(index)) {
                    h.buyMoment(bar.getClosePrice(), bar.getEndTime().toEpochSecond());
                } else {
                    boolean check = sell.check(train.get(j), 0, i, 1);
                    if (check)
                        h.sellMoment(bar.getClosePrice(), bar.getEndTime().toEpochSecond());
                }
            }
            System.out.println(i + ")" + h.all);
            if (h.all.profit > profit) {
                profit = h.all.profit;
                best = i;
            }
            if (h.all.count == 0) break;
        }
        System.out.println(String.format("best: %d (profit %.4g)", best, profit));
        return best;
    }

    private static void bestLimit(DecisionManager c, Instances train, int from, GustosBranches buy, GustosBranches sell) {
        int bestb = 1;
        int bests = 1;
        double profit = 1;
        PLHistory besth = null;
        for (int b = 2; b < 30; b++) {
            for (int s = 2; s < 50; s++) {
                PLHistory h = new PLHistory(c.data.instrument.toString(), null);
                for (int j = 0; j < train.size(); j++) {
                    int index = from + j;
                    XBar bar = c.data.bars.get(index);
                    if (sell.check(train.get(j), 0, s, 1))
                        h.sellMoment(bar.getClosePrice(), bar.getEndTime().toEpochSecond());
                    else if (buy.check(train.get(j), 0, b, 1))
                        h.buyMoment(bar.getClosePrice(), bar.getEndTime().toEpochSecond());

                }
                if (h.all.count >= 2 && h.all.profit > profit) {
                    profit = h.all.profit;
                    besth = h;
                    bestb = b;
                    bests = s;
                }
                if (h.all.count == 0) break;
            }
        }
        if (besth != null) {
            System.out.println(String.format("best in validation %d %d: %s", bestb, bests, besth.all.toString()));
            buy.limit = bestb;
            sell.limit = bests;
        } else {
            System.out.println("bad on validation.");
            buy.limit = 100;
            sell.limit = 100;
        }
    }


}


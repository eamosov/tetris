package ru.gustos.trading.tests;

import ru.efreet.trading.exchange.Instrument;
import ru.gustos.trading.book.SheetUtils;
import ru.gustos.trading.global.*;
import weka.classifiers.CostMatrix;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.PriorityQueue;

public class TestBuySell {

    public static void main(String[] args) throws Exception {
        Instrument instr = Instrument.getETH_USDT();
        Global global = TestGlobal.init(new Instrument[]{instr});
        DecisionCalc.DETECTOR = true;
        InstrumentData data = global.getInstrument(instr.toString());
        DecisionManager c = new DecisionManager(null,data, 6, true, 0);


        System.out.println("buy");
        fillBuySell(data, true);
        makeSteps(data,0);

//        makeTestTrees(data, 0);
//        System.out.println("\n\nsell");
//        fillBuySell(data, false);
//        makeTestTrees(data, 1);



        TestGlobal.saveResults(global);

    }

    private static void makeTestTrees(InstrumentData data, int att) throws Exception {
        Instances set = data.helper2.makeSet(data.data2(), null, DecisionManager.calcAllFrom, data.size()-200, Long.MAX_VALUE, att, 9,0);
        int cnt = set.size();
        for (int i = 1;i<100;i++) {
            Instances train = new Instances(set,(i-1)*cnt/8,cnt/8);
//            train.removeIf(inst->inst.weight()<1);

            Instances test = new Instances(set,i*cnt/8+200,cnt/8-200);
            System.out.println("cv "+i+", train size "+train.size());
//            System.out.println("test  "+CalcUtils.weightWithValue(test,test.classIndex(),0)+"/"+CalcUtils.weightWithValue(test,test.classIndex(),1));

            RandomForestWithExam rf = new RandomForestWithExam();
            rf.setNumExecutionSlots(4);
            rf.setMaxDepth(10);
            rf.setNumIterations(500);
            rf.buildClassifier(train);

            PriorityQueue<RandomTreeWithExam.Branch> queue = new PriorityQueue<>();
//            System.out.println("oks");
            rf.findGoodBranches(CalcUtils.weightWithValue(train,train.classIndex(),1)/50.0,queue,100, 1);
            int oks = 0;
            for (RandomTreeWithExam.Branch b : queue) {
                oks += b.test(test)? 1:0;
//                System.out.println(b.toString());
            }
            System.out.println("oks correct: "+oks);
//            System.out.println("not oks");
            queue.clear();
            rf.findGoodBranches(CalcUtils.weightWithValue(train,train.classIndex(),0)/50.0,queue,100, 0);
            oks = 0;
            for (RandomTreeWithExam.Branch b : queue) {
                oks += b.test(test)?1:0;
//                System.out.println(b.toString());
            }
            System.out.println("not oks correct: "+oks);

//            CostMatrix cm = new CostMatrix(2);
//            cm.setElement(0,1,2);
//            Evaluation eval = new Evaluation(test);
//            eval.evaluateModel(rf, test);
//            System.out.println(eval.kappa());
//            System.out.println(Arrays.deepToString(eval.confusionMatrix()));
        }
    }

    private static void makeSteps(InstrumentData data, int att) throws Exception {
        Instances set = data.helper2.makeSet(data.data2(), null, DecisionManager.calcAllFrom, data.size()-200, Long.MAX_VALUE, att, 9,0);
        int cnt = set.size();
        int learn = 24*60*14;
        int exam = 24*60;
        int step = 60;
        int pos = learn+60*24*60;
        File oksf = new File("oks");
        File notoksf = new File("notoks");
        try {
            oksf.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            notoksf.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        while (pos+exam<set.size()) {
            Instances train = new Instances(set,pos-learn,learn-200);
//            train.removeIf(inst->inst.weight()<1);

            Instances test = new Instances(set,pos,exam);
//            System.out.println("cv "+i+", train size "+train.size());
//            System.out.println("test  "+CalcUtils.weightWithValue(test,test.classIndex(),0)+"/"+CalcUtils.weightWithValue(test,test.classIndex(),1));

            RandomForestWithExam rf = new RandomForestWithExam();
            rf.setNumExecutionSlots(8);
            rf.setMaxDepth(20);
            rf.setNumIterations(500);
            rf.buildClassifier(train);

            PriorityQueue<RandomTreeWithExam.Branch> queue = new PriorityQueue<>();
//            System.out.println("oks");
            rf.findGoodBranches(CalcUtils.weightWithValue(train,train.classIndex(),1)/50.0,queue,100, 1);
            int oks = 0;
            for (RandomTreeWithExam.Branch b : queue) {
                oks += b.test(test)? 1:0;
//                System.out.println(b.toString());
            }
            try (FileWriter w = new FileWriter(oksf,true)){
                w.write(oks+" ");
            }
            queue.clear();
            rf.findGoodBranches(CalcUtils.weightWithValue(train,train.classIndex(),1)/50.0,queue,100, 0);
            int notoks = 0;
            for (RandomTreeWithExam.Branch b : queue) {
                notoks += b.test(test)? 1:0;
//                System.out.println(b.toString());
            }
            try (FileWriter w = new FileWriter(notoksf,true)){
                w.write(notoks+" ");
            }
            pos+=step;
//            CostMatrix cm = new CostMatrix(2);
//            cm.setElement(0,1,2);
//            Evaluation eval = new Evaluation(test);
//            eval.evaluateModel(rf, test);
//            System.out.println(eval.kappa());
//            System.out.println(Arrays.deepToString(eval.confusionMatrix()));
        }
    }

    private static void fillBuySell(InstrumentData data, boolean buy) {
        int w = 120;
        double limit = 0.00;
        int cbuy = 0, csell = 0;
        int cc = 0;
        PLHistory h = new PLHistory(data.instrument.toString(),null);
        for (int i = DecisionManager.calcAllFrom;i<data.size()-w;i++){
            double pp = SheetUtils.avgPrice(data, i, w);
            InstrumentMoment m = data.bars.get(i);
            boolean b = m.bar.getClosePrice() < pp * (1 - limit);
            boolean s = m.bar.getClosePrice() > pp * (1 + limit);
            if (b)
                cbuy++;
            if (s)
                csell++;
            if (b)
                h.buyMoment(m.bar.getClosePrice(),m.bar.getEndTime().toEpochSecond());
            if (s)
                h.sellMoment(m.bar.getClosePrice(),m.bar.getEndTime().toEpochSecond());
            data.helper2.put(m.mldata2,"_buy", b ?1:0, true);
            data.helper2.put(m.mldata2,"_sell", s ?1:0 , true);
            if (buy)
                m.mldata2.weight = (b? pp/m.bar.getClosePrice()-1:m.bar.getClosePrice()/pp-1)*100;
            else
                m.mldata2.weight = (!s ? pp/m.bar.getClosePrice()-1:m.bar.getClosePrice()/pp-1)*100;
            cc++;
        }

        System.out.println(String.format("buys: %d, sells: %d, total: %d, profit: %.3g (%.3g%%)", cbuy, csell, cc,h.all.profit,h.all.goodcount*100.0/h.all.count));
    }
}


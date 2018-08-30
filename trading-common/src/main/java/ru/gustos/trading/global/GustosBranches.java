package ru.gustos.trading.global;

import kotlin.Pair;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

public class GustosBranches {
    Instances set;
    ArrayList<RandomTreeWithExam.Branch> branchesTrue;
    ArrayList<RandomTreeWithExam.Branch> branchesFalse;
    public int limit;
    int cpus;

    public GustosBranches() {
        this(8);
    }

    public GustosBranches(int cpus) {
        this.cpus = cpus;
    }

    public void build(Instances set, int maxDepth, int trees) throws Exception {
        this.set = set;
        RandomForestWithExam f = new RandomForestWithExam();
        f.setMaxDepth(maxDepth);
        f.setNumExecutionSlots(cpus);
        f.setNumIterations(trees);
        f.buildClassifier(set);

        PriorityQueue<RandomTreeWithExam.Branch> queue = new PriorityQueue<>();
        f.findGoodBranches(CalcUtils.weightWithValue(set, set.classIndex(), 1) / 50.0, queue, 100, 1);
        branchesTrue = new ArrayList<>(queue);
        queue.clear();
        f.findGoodBranches(CalcUtils.weightWithValue(set, set.classIndex(), 1) / 50.0, queue, 100, 0);
        branchesFalse = new ArrayList<>(queue);
    }

    public void buildSelectingAttributes(Instances set, int selectDepth, int maxDepth, int trees) throws Exception {
        this.set = set;
        RandomForestWithExam f = new RandomForestWithExam();
        f.setMaxDepth(selectDepth);
        f.setNumExecutionSlots(cpus);
        f.setNumIterations(trees);
        int trainSize = set.size() * 2 / 3;
        f.buildClassifier(new Instances(set,0, trainSize));


        PriorityQueue<RandomTreeWithExam.Branch> queue = new PriorityQueue<>();
        f.findGoodBranches(CalcUtils.weightWithValue(set, set.classIndex(), 1) / 50.0, queue, 100, 1);
        ArrayList<RandomTreeWithExam.Branch> trueBranches = new ArrayList<>(queue);
        double[] pizd = new double[set.numAttributes()-1];
        double[] oks = new double[set.numAttributes()-1];
        for (RandomTreeWithExam.Branch b : trueBranches){
            for (int i = trainSize; i<set.size(); i++) {
                Instance inst = set.instance(i);
                if (inst.value(inst.classIndex())==0)
                    b.collectPizdunstvo(b.check(inst,0)?pizd:oks, inst.weight());
            }
        }
        ArrayList<Pair<Integer,Double>> atts = new ArrayList<>();
        int cntzero = 0;
        for (int i = 0;i<pizd.length;i++) {
            double w = pizd[i] + oks[i];
            double v = w < 0.0001 ? 100000 : pizd[i] / w;
            if (v==100000)
                cntzero++;
            atts.add(new Pair<>(i, v));
        }
        atts.sort(Comparator.comparing(Pair::getSecond));
        atts.subList(0,(atts.size()-cntzero)*4/5).clear();
//        for (int i = 0;i<atts.size();i++)
//            System.out.println("toignore: "+set.attribute(atts.get(i).getFirst()).name()+" "+atts.get(i).getSecond());

        int[] ignores = atts.stream().mapToInt(Pair<Integer, Double>::getFirst).toArray();
        for (int i = 0;i<set.size();i++){
            Instance inst = set.instance(i);
            for (int j =0;j<ignores.length;j++){
                inst.setValue(ignores[j],0);
            }
        }
        build(set,maxDepth,trees);
    }

    public boolean check(Instance inst, int canBeWrong, int minGoods, int maxBads) {
        int goods = 0;
        int bads = 0;
        for (int i = 0; i < branchesTrue.size(); i++) {
            RandomTreeWithExam.Branch branch = branchesTrue.get(i);
            if (!branch.penaltied && branch.check(inst,canBeWrong))
                goods++;
        }
        if (goods < minGoods) return false;
        for (int i = 0; i < branchesFalse.size(); i++)
            if (branchesFalse.get(i).check(inst,canBeWrong))
                bads++;
        if (bads >= maxBads) return false;
        return true;
    }

    public boolean check(Instance inst){
        return check(inst,0,limit,1);
    }


    public void correctBad(Instance inst) {
        for (int i = 0; i < branchesTrue.size(); i++)
            if (branchesTrue.get(i).check(inst,0)){
                branchesTrue.get(i).penaltied = true;
            }

    }
}

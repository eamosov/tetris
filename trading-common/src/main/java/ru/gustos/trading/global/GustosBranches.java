package ru.gustos.trading.global;

import kotlin.Pair;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;

import java.util.*;
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
        f.findGoodBranches(CalcUtils.weightWithValue(set, set.classIndex(), 1) / 30.0, queue, 100, 1);
        branchesTrue = new ArrayList<>(queue);
        queue.clear();
        f.findGoodBranches(CalcUtils.weightWithValue(set, set.classIndex(), 1) / 30.0, queue, 100, 0);
        branchesFalse = new ArrayList<>(queue);
    }

    public void printBranchStats(String prefix, Instances set) {
        double[] clarity = branchesTrue.stream().mapToDouble(b -> b.p).toArray();
        Arrays.sort(clarity);
        int correct = 0;
        int skipped = 0;
        int error = 0;
        double correctW = 0;
        double skippedW = 0;
        double errorW = 0;
        for (int i = 0;i<set.size();i++){
            Instance ii = set.instance(i);
            boolean check = check(ii,0,1,1000);
            boolean real = ii.value(ii.classIndex())>0.5;
            if (check && real) {
                correct++;
                correctW+=ii.weight();
            }
            if (check && !real) {
                error++;
                errorW+=ii.weight();
            }
            if (!check && real) {
                skipped++;
                skippedW+=ii.weight();
            }
        }
        System.out.println(String.format("%s branch stats: clarity %.4g/%.4g/%.4g. true/false/skip=%d/%d/%d (%d/%d/%d)", prefix, 1-clarity[0],1-clarity[clarity.length/2],1-clarity[clarity.length-1],correct,error,skipped,(int)correctW,(int)errorW,(int)skippedW));
    }

    public void prepareSet(Instances set, int maxDepth, int trees, double partInOne) throws Exception {
        int size = (int)(set.size()*partInOne);
        HashSet<Integer> attr = new HashSet<>();
        trees = trees/4;
        for (int t = 0;t<trees;t++){
            RandomTreeWithExam tree = new RandomTreeWithExam();
            tree.setKValue(10000);
            tree.setMaxDepth(maxDepth);
            Instances subset = new Instances(set, (set.size() - size) * t / (trees - 1), size);
            tree.buildClassifier(subset);
            tree.collectAttributes(attr);
        }
        System.out.println(String.format("take %d attributes of %d", attr.size(),set.numAttributes()-1));
        for (int i = 0;i<set.size();i++){
            Instance inst = set.get(i);
            for (int j = 0;j<inst.numAttributes()-1;j++)
                if (!attr.contains(j))
                    inst.setValue(j,0);
        }
    }
    public void buildSelectingAttributes(Instances set, int selectDepth, int maxDepth, int trees) throws Exception {
        prepareSet(set,selectDepth,trees,0.1);
        build(set,maxDepth,trees);
    }

    private void buildSelectingAttributes(Instances set, int selectDepth, int maxDepth, int trees, int deep) throws Exception {
        this.set = set;
        RandomForestWithExam f;
        f = new RandomForestWithExam();
        f.setMaxDepth(selectDepth);
        f.setNumExecutionSlots(cpus);
        f.setNumIterations(trees);
        int trainSize = set.size() * 2 / 3;
        f.buildClassifier(new Instances(set, 0, trainSize));


        PriorityQueue<RandomTreeWithExam.Branch> queue = new PriorityQueue<>();
        f.findGoodBranches(CalcUtils.weightWithValue(set, set.classIndex(), 1) / 50.0, queue, 100, 1);
        ArrayList<RandomTreeWithExam.Branch> trueBranches = new ArrayList<>(queue);
        double[] pizd = new double[set.numAttributes() - 1];
        double[] oks = new double[set.numAttributes() - 1];
        double[] counts = new double[set.numAttributes() - 1];
        for (RandomTreeWithExam.Branch b : trueBranches) {
            int[] attributes = b.attributes();
            for (int i = trainSize; i < set.size(); i++) {
                Instance inst = set.instance(i);
                boolean realValue = inst.value(inst.classIndex()) > 0;
                boolean branchValue = b.check(inst, 0);
                for (int bi = 0; bi < attributes.length; bi++) {
                    int a = attributes[bi];
                    boolean skippedValue = b.checkSkipping(inst, a);
                    counts[a] += inst.weight();
                    if (realValue && !branchValue) pizd[a] += inst.weight()*(skippedValue?3:1);
                    if (!realValue && branchValue) pizd[a] += inst.weight();
                    if (!realValue && !branchValue) oks[a] += inst.weight()*(skippedValue?3:1);
                    if (realValue && branchValue) oks[a]+=inst.weight();
                }
            }
        }
        ArrayList<Pair<Integer, Double>> atts = new ArrayList<>();
        for (int i = 0; i < pizd.length; i++) {
            double w = pizd[i] + oks[i];
            double v = w < 0.0001 ? 0 : pizd[i] / w;
            atts.add(new Pair<>(i, v));
        }

        atts.sort(Comparator.comparing(Pair::getSecond));
//        if (deep == 0) {
//            for (int i = 0; i < atts.size(); i++) {
//                int a = atts.get(i).getFirst();
//                System.out.println(String.format("attribute %s: %.4g %d/%d", set.attribute(a).name(), atts.get(i).getSecond(), (int) (oks[a] + pizd[a]), (int) counts[a]));
//            }
//        }

        atts.subList(0,atts.size()*9/10).clear();
// очищаем значения плохих признаков чтобы вообще не ипользовались
//        int[] ignores = atts.stream().filter(a -> a.getSecond() > 0.9).mapToInt(Pair<Integer, Double>::getFirst).toArray();
        int[] ignores = atts.stream().mapToInt(Pair<Integer, Double>::getFirst).toArray();
        for (int i = 0; i < set.size(); i++) {
            Instance inst = set.instance(i);
            for (int j = 0; j < ignores.length; j++) {
                inst.setValue(ignores[j], 0);
            }
        }

        if (deep == 0) {
            build(set, maxDepth, trees);
        } else
            buildSelectingAttributes(set, selectDepth, maxDepth, trees, deep - 1);

// убираем использование плохих признаков из веток
//        Set<Integer> ignore = atts.stream().filter(a -> a.getSecond() > 0.9).mapToInt(Pair<Integer, Double>::getFirst).mapToObj(Integer::new).collect(Collectors.toSet());
//
//        for (RandomTreeWithExam.Branch b : trueBranches)
//            b.removeAttribute(ignore);


    }

    public boolean check(Instance inst, int canBeWrong, int minGoods, int maxBads) {
        int goods = 0;
        int bads = 0;
        for (int i = 0; i < branchesTrue.size(); i++) {
            RandomTreeWithExam.Branch branch = branchesTrue.get(i);
            if (!branch.penaltied && branch.check(inst, canBeWrong)) {
                goods++;
                if (goods>=minGoods) break;
            }
        }
        if (goods < minGoods) return false;
        if (maxBads<branchesFalse.size()) {
            for (int i = 0; i < branchesFalse.size(); i++)
                if (branchesFalse.get(i).check(inst, canBeWrong))
                    bads++;
            if (bads >= maxBads) return false;
        }
        return true;
    }

    public boolean check(Instance inst) {
//        return check(inst, 0, 2, 100);
        return check(inst, 0, limit, 1);
    }


    public void correctBad(Instance inst) {
        for (int i = 0; i < branchesTrue.size(); i++)
            if (branchesTrue.get(i).check(inst, 0)) {
                branchesTrue.get(i).penaltied = true;
            }

    }
}


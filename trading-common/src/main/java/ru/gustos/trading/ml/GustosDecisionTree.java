package ru.gustos.trading.ml;

import weka.classifiers.AbstractClassifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class GustosDecisionTree extends AbstractClassifier {
    private int maxDepth = 1000;
    private Node root;
    private int minNode = 1;
    private int maxInNode = 2;
    private double enoughPurity = 0;
    private Instances info;


    public GustosDecisionTree() {
    }

    public double[] distributionForInstance(Instance inst) {
        double[] distribution = root.distribution(inst);
        double[] sums = distribution.clone();
        if (Utils.eq(Utils.sum(sums), 0.0D))
            return sums;
        else {
            Utils.normalize(sums);
            return sums;
        }
    }

    @Override
    public void buildClassifier(Instances set) throws Exception {
        info = set;
        root = new Node();
        set = new Instances(set,0,set.size());
        Instances train = set;
//        Random random = new Random();
//        set.randomize(random);
//        int numFolds = 4;
//        set.stratify(numFolds);
//        Instances train = set.trainCV(numFolds, 0);
//        Instances test = set.testCV(numFolds, 0);
//        maxDepth = 4;
//        minNode = set.size() / 20;
        root.build(train);
        root.buildSubs(train, maxDepth);
//        for (int i = 0;i<test.size();i++)
//            root.errors(test.get(i));
//        root.prune();

    }

    public void setMaxDepth(int depth) {
        maxDepth = depth;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        root.toString(sb,0);
        sb.append("leaves = ").append(root.countLeaves()).append("\n");
        sb.append("nodes = ").append(root.countNodes()).append("\n");
        return sb.toString();
    }

    class Node {
        ArrayList<Condition> conditions;
        int loLimit;
        int hiLimit;

        double[] confusionMatrix = new double[4];
        double[] distrib = new double[2];

        Node trueChild;
        Node falseChild;

        double errHere, okHere;
        double errDeep, okDeep;


        public double[] distribution(Instance inst) {
            if (conditions == null)
                return distrib;

            if (calc(inst))
                return trueChild.distribution(inst);
            else
                return falseChild.distribution(inst);
        }

        public double[] errors(Instance inst) {
            boolean rv = inst.value(inst.classIndex()) > 0.5;
            boolean v = distrib[1] > distrib[0];
            if (v == rv)
                okHere += inst.weight();
            else
                errHere += inst.weight();

            if (trueChild != null) {
                double[] dist;
                if (calc(inst))
                    dist = trueChild.errors(inst);
                else
                    dist = falseChild.errors(inst);

                v = dist[1] > dist[0];
                if (v == rv)
                    okDeep += inst.weight();
                else
                    errDeep += inst.weight();
                return dist;
            }
            return distrib;
        }

        public void prune() {
            if (conditions == null) return;
            trueChild.prune();
            falseChild.prune();
            if (trueChild.conditions == null && falseChild.conditions == null && okHere / (okHere + errHere) > trueChild.okHere+falseChild.okHere / (trueChild.okHere+falseChild.okHere + trueChild.errHere+falseChild.errHere)) {
                conditions = null;
                trueChild = null;
                falseChild = null;
            }
        }

        public int countNodes(){
            int n = 1;
            if (trueChild!=null) {
                n += trueChild.countNodes();
                n += falseChild.countNodes();
            }
            return n;
        }

        public int countLeaves(){
            int n = 0;
            if (trueChild!=null) {
                n += trueChild.countLeaves();
                n += falseChild.countLeaves();
            } else
                n = 1;
            return n;


        }


        private boolean calc(Instance inst) {
            int c = 0;
            for (int i = 0; i < conditions.size(); i++) {
                if (conditions.get(i).check(inst))
                    c++;
            }
            return c >= loLimit && c <= hiLimit;
        }

        public void build(Instances set) {
            conditions = null;
            calcDistrib(set);
            if (distrib[0] == 0 || distrib[1] == 0)
                return;

            Condition[] conds = new Condition[set.numAttributes() - 1];
            int n = 0;
            for (int att = 0; att < set.numAttributes(); att++)
                if (att != set.classIndex())
                    conds[n++] = new Condition(att, set);

            Arrays.sort(conds, (c1, c2) -> Double.compare(c2.gain, c1.gain));

            conditions = new ArrayList<>();
            conditions.add(conds[0]);
            loLimit = 1;
            hiLimit = 1;
            double gain = gain(set);
            if (gain <= 0) {
                conditions = null;
                return;
            }

            while (conditions.size()<maxInNode && tryAddCond(set, conds)) ;
            gain(set);
        }

        void buildSubs(Instances set, int maxdepth) {
            if (conditions == null) return;
            Instances t = new Instances(set, 10);
            Instances f = new Instances(set, 10);
            double t0 = 0,t1 = 0,f0 = 0,f1 = 0;
            for (int i = 0; i < set.size(); i++) {
                Instance ii = set.get(i);
                if (calc(ii)) {
                    t.add(ii);
                    if (ii.value(ii.classIndex())>0.5)
                        t1+=ii.weight();
                    else
                        t0+=ii.weight();
                }else {
                    f.add(ii);
                    if (ii.value(ii.classIndex())>0.5)
                        f1+=ii.weight();
                    else
                        f0+=ii.weight();
                }
            }
//            if (t.size()<minNode || f.size()<minNode){
//                calcDistrib(set);
//                conditions = null;
//                return;
//            }
            trueChild = new Node();
            falseChild = new Node();
            if (t.size() < GustosDecisionTree.this.minNode || maxdepth <= 1 || Math.min(t0,t1)/(t0+t1)<enoughPurity)
                trueChild.calcDistrib(t);
            else {
                trueChild.build(t);
                trueChild.buildSubs(t, maxdepth - 1);
            }
            if (f.size() < GustosDecisionTree.this.minNode || maxdepth <= 1 || Math.min(f0,f1)/(f0+f1)<enoughPurity)
                falseChild.calcDistrib(f);
            else {
                falseChild.build(f);
                falseChild.buildSubs(f, maxdepth - 1);
            }
        }

        private boolean tryAddCond(Instances set, Condition[] conds) {
            double bgain = gain(set);
            int prlo = loLimit;
            int prhi = hiLimit;
            int best = 0;
            int bestlo = 0, besthi = 0;
            for (int i = 1; i < conds.length; i++) {
                conditions.add(conds[i]);
                for (int lo = 0; lo <= conditions.size(); lo++)
                    for (int hi = lo; hi <= conditions.size(); hi++) {
                        loLimit = lo;
                        hiLimit = hi;
                        double nugain = gain(set)/20.1;
                        if (nugain > bgain) {
                            bgain = nugain;
                            best = i;
                            bestlo = lo;
                            besthi = hi;
                        }
                    }

                loLimit = prlo;
                hiLimit = prhi;
                conditions.remove(conditions.size() - 1);
            }
            if (best != 0) {
//            System.out.println("YEAH");
                conditions.add(conds[best]);
                loLimit = bestlo;
                hiLimit = besthi;
                return true;
            }
            return false;
        }

        private void calcDistrib(Instances set) {
            distrib[0] = 0;
            distrib[1] = 0;
            for (int i = 0; i < set.size(); i++) {
                Instance ii = set.get(i);
                if (ii.value(set.classIndex()) > 0.5)
                    distrib[1] += ii.weight();
                else
                    distrib[0] += ii.weight();
            }
        }

        private void makeCm(Instances set, double[] confusionMatrix) {
            Arrays.fill(confusionMatrix,0);
            for (int i = 0; i < set.size(); i++) {
                Instance ii = set.get(i);
                boolean v = calc(ii);
                boolean rv = ii.value(set.classIndex()) > 0.5;
                if (v && rv)
                    confusionMatrix[3] += ii.weight();
                else if (!v && !rv)
                    confusionMatrix[0] += ii.weight();
                else if (v && !rv)
                    confusionMatrix[1] += ii.weight();
                else if (!v && rv)
                    confusionMatrix[2] += ii.weight();
            }
        }

        double gain(Instances set) {
            makeCm(set, confusionMatrix);
            double in1 = confusionMatrix[3];
            double in0 = confusionMatrix[1];
            double out1 = confusionMatrix[2];
            double out0 = confusionMatrix[0];
            double total0 = in0 + out0;
            double total1 = in1 + out1;
            double total = total0 + total1;
            double before = GDTUtils.entropy(total0, total1);
            double in = GDTUtils.entropy(in0, in1);
            double out = GDTUtils.entropy(out0, out1);
            double after = (in0 + in1) / total * in + (out0 + out1) / total * out;
            return before - after;
        }

        public void toString(StringBuilder sb, int depth) {
            for (int i = 0;i<depth;i++)
                sb.append("|   ");
            if (conditions!=null) {
                sb.append(loLimit).append(" to ").append(hiLimit).append(" [");
                boolean f = false;
                for (Condition c : conditions) {
                    if (f)
                        sb.append(" & ");
                    else
                        f = true;
                    String name = GustosDecisionTree.this.info.attribute(c.attribute).name();
                    if (c.inner) {
                        if (c.loLimit!=-Double.MAX_VALUE)
                            sb.append(c.loLimit).append(" <= ");
                        sb.append(name);
                        if (c.hiLimit!=Double.MAX_VALUE)
                            sb.append(" <= ").append(c.hiLimit);
                    } else {
                        if (c.loLimit!=-Double.MAX_VALUE)
                            sb.append(name).append(" <= ").append(c.loLimit).append(" ");
                        if (c.hiLimit!=Double.MAX_VALUE)
                            sb.append(c.hiLimit).append(" <= ").append(name);
                    }
                }
                sb.append("] -> ").append(trueChild.distrib[1]).append("/").append(trueChild.distrib[0]).append("\n");
                trueChild.toString(sb, depth + 1);
                for (int i = 0; i < depth; i++)
                    sb.append("|   ");
                sb.append("else - > ").append(falseChild.distrib[1]).append("/").append(falseChild.distrib[0]).append("\n");;
                falseChild.toString(sb, depth + 1);
            } else {
                sb.append(distrib[1]).append("/").append(distrib[0]).append("\n");
            }
        }
    }

    class Condition {
        int attribute;
        double loLimit;
        double hiLimit;
        boolean inner;

        double gain;

        public Condition(int attribute, Instances set) {
            this.attribute = attribute;
            BestSplitFinder split = new BestSplitFinder(set, attribute);
            loLimit = split.lo;
            hiLimit = split.hi;
            inner = split.inner;
            gain = split.inc;
        }


        public boolean check(Instance inst) {
            double v = inst.value(attribute);
            boolean res = v >= loLimit && v <= hiLimit;
            if (!inner) res = !res;
            return res;
        }

    }

    class BestSplitFinder {
        double[] class0;
        double[] class1;
        int left, right;

        int step;

        double in1, in0, out1, out0;

        double lo, hi;
        double inc;
        boolean inner;

        BestSplitFinder(Instances set, int attribute) {
            set.sort(attribute);
            int numDistinctValues = set.numDistinctValues(attribute);
            class0 = new double[numDistinctValues];
            class1 = new double[numDistinctValues];
            double[] values = new double[numDistinctValues];
            int n = -1;
            for (int i = 0; i < set.size(); i++) {
                Instance ii = set.get(i);

                if (n==-1 || ii.value(attribute) != values[n]) {
                    n++;
                    values[n] = ii.value(attribute);
                }
                int v = ii.value(set.classIndex()) == 0 ? 0 : 1;
                if (v == 0)
                    class0[n] += ii.weight();
                else
                    class1[n] += ii.weight();
            }
            left = 0;
            right = class0.length - 1;

            int bl = left,br = right;
            double bgain = 0.0001;
            for (left = 0;left<class0.length;left++)
                for (right = left;right<class0.length;right++){
                    double g = gain();
                    if (left!=0 && right!=class0.length-1) g/=20;
                    if (g>bgain){
                        bgain = g;
                        bl = left;
                        br = right;
                    }
                }
                left = bl;
                right = br;
//                inc = bgain;


//            step = Math.max(1, right / 4);
//            do {
//                double gainInc = 0;
//                do {
//                    gainInc = makeBestStep();
//                } while (gainInc > 0);
//                step /= 2;
//            } while (step > 0);
//
            gain();
            inc = bgain;//gain();
            lo = left==0?-Double.MAX_VALUE:values[left];
            hi = right==values.length-1?Double.MAX_VALUE:values[right];
//            lo = left==0?-Double.MAX_VALUE:(values[left]+values[left-1])/2;
//            hi = right==values.length-1?Double.MAX_VALUE:(values[right]+values[right+1])/2;
            inner = in1 / (in1 + in0) > out1 / (out1 + out0);
        }

        private double makeBestStep() {
            double bgain = gain();
            double startGain = bgain;
            int bl = left;
            int br = right;
            int wl = left;
            int wr = right;
            int max = class0.length - 1;
            for (int l = -8; l <= 8; l++)
                for (int r = -8; r <= 8; r++) {
                    if (l == 0 && r == 0) continue;
                    if (l < 0 && left == 0) continue;
                    if (r > 0 && right == max) continue;

                    left = wl + l * step;
                    if (left < 0) left = 0;

                    right = wr + r * step;
                    if (right >= max) right = max;

                    if (left >= right) continue;

                    double g = gain();
                    if (g > bgain) {
                        bgain = g;
                        bl = left;
                        br = right;
                    }
                }
            left = bl;
            right = br;
            return bgain - startGain;
        }

        double gain() {
            in1 = 0;
            in0 = 0;
            out1 = 0;
            out0 = 0;
            for (int i = 0; i < left; i++) {
                out0 += class0[i];
                out1 += class1[i];
            }
            for (int i = left; i <= right; i++) {
                in0 += class0[i];
                in1 += class1[i];
            }
            for (int i = right + 1; i < class0.length; i++) {
                out0 += class0[i];
                out1 += class1[i];
            }
            double total0 = in0 + out0;
            double total1 = in1 + out1;
            double total = total0 + total1;
//            double before = GDTUtils.entropy(total0, total1);
//            double in = GDTUtils.entropy(in0, in1);
//            double out = GDTUtils.entropy(out0, out1);
            double before = 1 - total0*total0/total/total - total1*total1/total/total;
            double in = 1 - in0*in0/(in0+in1)/(in0+in1) - in1*in1/(in0+in1)/(in0+in1);
            double out = 1 - out0*out0/(out0+out1)/(out0+out1) - out1*out1/(out0+out1)/(out0+out1);

            double after = (in0 + in1) / total * in + (out0 + out1) / total * out;

            return before - after;
        }

    }

}


class GDTUtils {

    public static double entropy(double v1, double v2) {
        double t = v1 + v2;
        if (t == 0) return 0;
        double p1 = v1 / t;
        double p2 = v2 / t;
        return -p1 * log(p1) - p2 * log(p2);
    }

    private static double log(double v) {
        if (v <= 0)
            return 0;
        return Math.log(v);
    }
}



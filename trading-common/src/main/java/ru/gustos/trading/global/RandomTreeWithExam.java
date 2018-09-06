package ru.gustos.trading.global;

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import ru.gustos.trading.book.SheetUtils;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.rules.ZeroR;
import weka.core.*;
import weka.gui.ProgrammaticProperty;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class RandomTreeWithExam extends AbstractClassifier implements OptionHandler, WeightedInstancesHandler, Randomizable, Drawable, PartitionGenerator {
    private static final long serialVersionUID = -9051119597407396024L;
    protected RandomTreeWithExam.Tree m_Tree = null;
    protected Instances m_Info = null;
    protected double m_MinNum = 1.0D;
    protected int m_KValue = 0;
    protected int m_randomSeed = 1;
    protected int m_MaxDepth = 0;
    protected int m_NumFolds = 0;
    protected boolean m_AllowUnclassifiedInstances = false;
    protected boolean m_BreakTiesRandomly = false;
    protected Classifier m_zeroR;
    protected double m_MinVarianceProp = 0.001D;
    protected boolean m_computeImpurityDecreases;
    protected double[][] m_impurityDecreasees;
    protected int goodness = 10;

    public RandomTreeWithExam() {
    }

    public String globalInfo() {
        return "Class for constructing a tree that considers K randomly  chosen attributes at each node. Performs no pruning. Also has an option to allow estimation of class probabilities (or target mean in the regression case) based on a hold-out set (backfitting).";
    }

    public double[][] getImpurityDecreases() {
        return this.m_impurityDecreasees;
    }

    @ProgrammaticProperty
    public void setComputeImpurityDecreases(boolean computeImpurityDecreases) {
        this.m_computeImpurityDecreases = computeImpurityDecreases;
    }

    public boolean getComputeImpurityDecreases() {
        return this.m_computeImpurityDecreases;
    }

    public String minNumTipText() {
        return "The minimum total weight of the instances in a leaf.";
    }

    public double getMinNum() {
        return this.m_MinNum;
    }

    public void setMinNum(double newMinNum) {
        this.m_MinNum = newMinNum;
    }

    public String minVariancePropTipText() {
        return "The minimum proportion of the variance on all the data that needs to be present at a node in order for splitting to be performed in regression trees.";
    }

    public double getMinVarianceProp() {
        return this.m_MinVarianceProp;
    }

    public void setMinVarianceProp(double newMinVarianceProp) {
        this.m_MinVarianceProp = newMinVarianceProp;
    }

    public String KValueTipText() {
        return "Sets the number of randomly chosen attributes. If 0, int(log_2(#predictors) + 1) is used.";
    }

    public int getKValue() {
        return this.m_KValue;
    }

    public void setKValue(int k) {
        this.m_KValue = k;
    }

    public String seedTipText() {
        return "The random number seed used for selecting attributes.";
    }

    public void setSeed(int seed) {
        this.m_randomSeed = seed;
    }

    public int getSeed() {
        return this.m_randomSeed;
    }

    public String maxDepthTipText() {
        return "The maximum depth of the tree, 0 for unlimited.";
    }

    public int getMaxDepth() {
        return this.m_MaxDepth;
    }

    public void setMaxDepth(int value) {
        this.m_MaxDepth = value;
    }

    public String numFoldsTipText() {
        return "Determines the amount of data used for backfitting. One fold is used for backfitting, the rest for growing the tree. (Default: 0, no backfitting)";
    }

    public int getNumFolds() {
        return this.m_NumFolds;
    }

    public void setNumFolds(int newNumFolds) {
        this.m_NumFolds = newNumFolds;
    }

    public String allowUnclassifiedInstancesTipText() {
        return "Whether to allow unclassified instances.";
    }

    public boolean getAllowUnclassifiedInstances() {
        return this.m_AllowUnclassifiedInstances;
    }

    public void setAllowUnclassifiedInstances(boolean newAllowUnclassifiedInstances) {
        this.m_AllowUnclassifiedInstances = newAllowUnclassifiedInstances;
    }

    public String breakTiesRandomlyTipText() {
        return "Break ties randomly when several attributes look equally good.";
    }

    public boolean getBreakTiesRandomly() {
        return this.m_BreakTiesRandomly;
    }

    public void setBreakTiesRandomly(boolean newBreakTiesRandomly) {
        this.m_BreakTiesRandomly = newBreakTiesRandomly;
    }

    public Enumeration<Option> listOptions() {
        Vector<Option> newVector = new Vector();
        newVector.addElement(new Option("\tNumber of attributes to randomly investigate.\t(default 0)\n\t(<1 = int(log_2(#predictors)+1)).", "K", 1, "-K <number of attributes>"));
        newVector.addElement(new Option("\tSet minimum number of instances per leaf.\n\t(default 1)", "M", 1, "-M <minimum number of instances>"));
        newVector.addElement(new Option("\tSet minimum numeric class variance proportion\n\tof train variance for split (default 1e-3).", "V", 1, "-V <minimum variance for split>"));
        newVector.addElement(new Option("\tSeed for random number generator.\n\t(default 1)", "S", 1, "-S <num>"));
        newVector.addElement(new Option("\tThe maximum depth of the tree, 0 for unlimited.\n\t(default 0)", "depth", 1, "-depth <num>"));
        newVector.addElement(new Option("\tNumber of folds for backfitting (default 0, no backfitting).", "N", 1, "-N <num>"));
        newVector.addElement(new Option("\tAllow unclassified instances.", "U", 0, "-U"));
        newVector.addElement(new Option("\t" + this.breakTiesRandomlyTipText(), "B", 0, "-B"));
        newVector.addAll(Collections.list(super.listOptions()));
        return newVector.elements();
    }

    public String[] getOptions() {
        Vector<String> result = new Vector();
        result.add("-K");
        result.add("" + this.getKValue());
        result.add("-M");
        result.add("" + this.getMinNum());
        result.add("-V");
        result.add("" + this.getMinVarianceProp());
        result.add("-S");
        result.add("" + this.getSeed());
        if (this.getMaxDepth() > 0) {
            result.add("-depth");
            result.add("" + this.getMaxDepth());
        }

        if (this.getNumFolds() > 0) {
            result.add("-N");
            result.add("" + this.getNumFolds());
        }

        if (this.getAllowUnclassifiedInstances()) {
            result.add("-U");
        }

        if (this.getBreakTiesRandomly()) {
            result.add("-B");
        }

        Collections.addAll(result, super.getOptions());
        return (String[])result.toArray(new String[result.size()]);
    }

    public void setOptions(String[] options) throws Exception {
        String tmpStr = Utils.getOption('K', options);
        if (tmpStr.length() != 0) {
            this.m_KValue = Integer.parseInt(tmpStr);
        } else {
            this.m_KValue = 0;
        }

        tmpStr = Utils.getOption('M', options);
        if (tmpStr.length() != 0) {
            this.m_MinNum = Double.parseDouble(tmpStr);
        } else {
            this.m_MinNum = 1.0D;
        }

        String minVarString = Utils.getOption('V', options);
        if (minVarString.length() != 0) {
            this.m_MinVarianceProp = Double.parseDouble(minVarString);
        } else {
            this.m_MinVarianceProp = 0.001D;
        }

        tmpStr = Utils.getOption('S', options);
        if (tmpStr.length() != 0) {
            this.setSeed(Integer.parseInt(tmpStr));
        } else {
            this.setSeed(1);
        }

        tmpStr = Utils.getOption("depth", options);
        if (tmpStr.length() != 0) {
            this.setMaxDepth(Integer.parseInt(tmpStr));
        } else {
            this.setMaxDepth(0);
        }

        String numFoldsString = Utils.getOption('N', options);
        if (numFoldsString.length() != 0) {
            this.m_NumFolds = Integer.parseInt(numFoldsString);
        } else {
            this.m_NumFolds = 0;
        }

        this.setAllowUnclassifiedInstances(Utils.getFlag('U', options));
        this.setBreakTiesRandomly(Utils.getFlag('B', options));
        super.setOptions(options);
        Utils.checkForRemainingOptions(options);
    }

    public Capabilities getCapabilities() {
        Capabilities result = super.getCapabilities();
        result.disableAll();
        result.enable(Capabilities.Capability.NOMINAL_ATTRIBUTES);
        result.enable(Capabilities.Capability.NUMERIC_ATTRIBUTES);
        result.enable(Capabilities.Capability.DATE_ATTRIBUTES);
        result.enable(Capabilities.Capability.MISSING_VALUES);
        result.enable(Capabilities.Capability.NOMINAL_CLASS);
        result.enable(Capabilities.Capability.NUMERIC_CLASS);
        result.enable(Capabilities.Capability.MISSING_CLASS_VALUES);
        return result;
    }

    public void buildClassifier(Instances data) throws Exception {
        if (this.m_computeImpurityDecreases) {
            this.m_impurityDecreasees = new double[data.numAttributes()][2];
        }

        if (this.m_KValue > data.numAttributes() - 1) {
            this.m_KValue = data.numAttributes() - 1;
        }

        if (this.m_KValue < 1) {
            this.m_KValue = (int)Utils.log2((double)(data.numAttributes() - 1)) + 1;
        }

        this.getCapabilities().testWithFail(data);
        data = new Instances(data);
        data.deleteWithMissingClass();
        if (data.numAttributes() == 1) {
            System.err.println("Cannot build model (only class attribute present in data!), using ZeroR model instead!");
            this.m_zeroR = new ZeroR();
            this.m_zeroR.buildClassifier(data);
        } else {
            this.m_zeroR = null;
            Instances train = null;
            Instances backfit = null;
            Random rand = data.getRandomNumberGenerator((long)this.m_randomSeed);
            if (this.m_NumFolds <= 0) {
                train = data;
            } else {
                data.randomize(rand);
                data.stratify(this.m_NumFolds);
                train = data.trainCV(this.m_NumFolds, 1, rand);
                backfit = data.testCV(this.m_NumFolds, 1);
            }

            int[] attIndicesWindow = new int[data.numAttributes() - 1];
            int j = 0;

            for(int i = 0; i < attIndicesWindow.length; ++i) {
                if (j == data.classIndex()) {
                    ++j;
                }

                attIndicesWindow[i] = j++;
            }

            double totalWeight = 0.0D;
            double totalSumSquared = 0.0D;
            double[] classProbs = new double[train.numClasses()];

            for(int i = 0; i < train.numInstances(); ++i) {
                Instance inst = train.instance(i);
                if (data.classAttribute().isNominal()) {
                    int var10001 = (int)inst.classValue();
                    classProbs[var10001] += inst.weight();
                    totalWeight += inst.weight();
                } else {
                    classProbs[0] += inst.classValue() * inst.weight();
                    totalSumSquared += inst.classValue() * inst.classValue() * inst.weight();
                    totalWeight += inst.weight();
                }
            }

            double trainVariance = 0.0D;
            if (data.classAttribute().isNumeric()) {
                trainVariance = singleVariance(classProbs[0], totalSumSquared, totalWeight) / totalWeight;
                classProbs[0] /= totalWeight;
            }

            this.m_Tree = new RandomTreeWithExam.Tree();
            this.m_Info = new Instances(data, 0);
            this.m_Tree.buildTree(train, classProbs, attIndicesWindow, totalWeight, rand, 0, this.m_MinVarianceProp * trainVariance);
            if (backfit != null) {
                this.m_Tree.backfitData(backfit);
            }

        }
    }

    public double[] distributionForInstance(Instance instance) throws Exception {
        return this.m_zeroR != null ? this.m_zeroR.distributionForInstance(instance) : this.m_Tree.distributionForInstance(instance);
    }

    public String toString() {
        if (this.m_zeroR != null) {
            StringBuffer buf = new StringBuffer();
            buf.append(this.getClass().getName().replaceAll(".*\\.", "") + "\n");
            buf.append(this.getClass().getName().replaceAll(".*\\.", "").replaceAll(".", "=") + "\n\n");
            buf.append("Warning: No model could be built, hence ZeroR model is used:\n\n");
            buf.append(this.m_zeroR.toString());
            return buf.toString();
        } else {
            return this.m_Tree == null ? "RandomTreeWithExam: no model has been built yet." : "\nRandomTreeWithExam\n==========\n" + this.m_Tree.toString(0) + "\n\nSize of the tree : " + this.m_Tree.numNodes() + (this.getMaxDepth() > 0 ? "\nMax depth of tree: " + this.getMaxDepth() : "");
        }
    }

    public String graph() throws Exception {
        if (this.m_Tree == null) {
            throw new Exception("RandomTreeWithExam: No model built yet.");
        } else {
            StringBuffer resultBuff = new StringBuffer();
            this.m_Tree.toGraph(resultBuff, 0, (RandomTreeWithExam.Tree)null);
            String result = "digraph RandomTreeWithExam {\nedge [style=bold]\n" + resultBuff.toString() + "\n}\n";
            return result;
        }
    }

    public int graphType() {
        return 1;
    }

    public void generatePartition(Instances data) throws Exception {
        this.buildClassifier(data);
    }

    public double[] getMembershipValues(Instance instance) throws Exception {
        double[] a;
        if (this.m_zeroR != null) {
            a = new double[]{instance.weight()};
            return a;
        } else {
            a = new double[this.numElements()];
            java.util.Queue<Double> queueOfWeights = new LinkedList();
            java.util.Queue<RandomTreeWithExam.Tree> queueOfNodes = new LinkedList();
            queueOfWeights.add(instance.weight());
            queueOfNodes.add(this.m_Tree);
            int index = 0;

            while(true) {
                RandomTreeWithExam.Tree node;
                do {
                    if (queueOfNodes.isEmpty()) {
                        return a;
                    }

                    a[index++] = (Double)queueOfWeights.poll();
                    node = (RandomTreeWithExam.Tree)queueOfNodes.poll();
                } while(node.m_Attribute <= -1);

                double[] weights = new double[node.m_Successors.length];
                if (instance.isMissing(node.m_Attribute)) {
                    System.arraycopy(node.m_Prop, 0, weights, 0, node.m_Prop.length);
                } else if (this.m_Info.attribute(node.m_Attribute).isNominal()) {
                    weights[(int)instance.value(node.m_Attribute)] = 1.0D;
                } else if (instance.value(node.m_Attribute) < node.m_SplitPoint) {
                    weights[0] = 1.0D;
                } else {
                    weights[1] = 1.0D;
                }

                for(int i = 0; i < node.m_Successors.length; ++i) {
                    queueOfNodes.add(node.m_Successors[i]);
                    queueOfWeights.add(a[index - 1] * weights[i]);
                }
            }
        }
    }

    public int numElements() throws Exception {
        return this.m_zeroR != null ? 1 : this.m_Tree.numNodes();
    }

    protected static double variance(double[] s, double[] sS, double[] sumOfWeights) {
        double var = 0.0D;

        for(int i = 0; i < s.length; ++i) {
            if (sumOfWeights[i] > 0.0D) {
                var += singleVariance(s[i], sS[i], sumOfWeights[i]);
            }
        }

        return var;
    }

    protected static double singleVariance(double s, double sS, double weight) {
        return sS - s * s / weight;
    }

    public static void main(String[] argv) {
        runClassifier(new RandomTreeWithExam(), argv);
    }

    public Pair<Integer, Boolean> computePizdunstvo(Instance inst) throws Exception {
        return m_Tree.pizdunstvo(inst);
    }

    ArrayList<Integer> pizdTemp = new ArrayList<>();
    public void computePizdunstvo2(Instance inst, double[][] pizdunstvo) throws Exception {
        pizdTemp.clear();
        m_Tree.collectAttributes(inst,pizdTemp);
        double r = classifyInstance(inst);
        boolean correct = r > 0.5 == inst.value(inst.classIndex()) > 0.5;
        for (int i = 0;i<pizdTemp.size();i++){
            int attr = pizdTemp.get(i);
            pizdunstvo[1][attr]+=1;
            if (!correct)
                pizdunstvo[0][attr]+=1;
        }
    }

    public void computeCombPizdunstvo(Instance inst) throws Exception {
        double r = classifyInstance(inst);
        boolean real = inst.value(inst.classIndex()) > 0.5;
        TreePizdunstvo.p.add(makeAttributesString(inst),real, r > 0.5);
    }

    public void updateCorrectness(Instance instance, boolean result) throws Exception {
        if (!result) {
            double[] distr = distributionForInstance(instance);
            boolean classified = distr[1] > 0.5;
            boolean good = result == classified;
            Tree leaf = m_Tree.getLeaf(instance);
            if (good) {
                goodness = Math.min(20, goodness + 5);
                leaf.goodness = Math.min(20, leaf.goodness + 5);
            } else {
                goodness = Math.max(0, goodness - 1);
                leaf.goodness = Math.max(0, leaf.goodness - 1);
            }
        }
    }

    public int goodness(Instance instance){

//        return m_Tree.getLeaf(instance).goodness+5;
//        return 10;
        return m_Tree.getLeaf(instance).goodness<3?0:10;
//        return goodness==0 || goodness>6?10:0;
    }

    public boolean isGoodFor(Instance instance){
        return goodness>3;
////        return true;
//        Tree leaf = m_Tree.getLeaf(instance);
////        System.out.println(leaf.goodness);
//        return leaf.goodness<13;
    }



    private String makeAttributesString(Instance inst) throws Exception {
        ArrayList<Integer> res = new ArrayList<>();
        m_Tree.collectAttributes(inst,res);
        return res.stream().map(i -> inst.attribute(i).name()).sorted().collect(Collectors.joining(","));
    }

    public void collectAttributes(HashSet<Integer> set){
        m_Tree.collectAttributes(set);
    }

    public void findGoodBranches(double minWeight, PriorityQueue<Branch> collect, int count, int maxClass) {
        m_Tree.findGoodBranches(minWeight, collect, count, maxClass, new Branch(m_Info));

    }

    protected class Tree implements Serializable {
        private static final long serialVersionUID = 3549573538656522569L;
        protected RandomTreeWithExam.Tree[] m_Successors;
        protected int m_Attribute = -1;
        protected double m_SplitPoint = 0.0D / 0.0;
        protected double[] m_Prop = null;
        protected double[] m_ClassDistribution = null;
        protected double[] m_Distribution = null;

        protected int goodness = 10;

        protected Tree() {
        }

        public void backfitData(Instances data) throws Exception {
            double totalWeight = 0.0D;
            double totalSumSquared = 0.0D;
            double[] classProbs = new double[data.numClasses()];

            for(int i = 0; i < data.numInstances(); ++i) {
                Instance inst = data.instance(i);
                if (data.classAttribute().isNominal()) {
                    int var10001 = (int)inst.classValue();
                    classProbs[var10001] += inst.weight();
                    totalWeight += inst.weight();
                } else {
                    classProbs[0] += inst.classValue() * inst.weight();
                    totalSumSquared += inst.classValue() * inst.classValue() * inst.weight();
                    totalWeight += inst.weight();
                }
            }

            double trainVariance = 0.0D;
            if (data.classAttribute().isNumeric()) {
                trainVariance = RandomTreeWithExam.singleVariance(classProbs[0], totalSumSquared, totalWeight) / totalWeight;
                classProbs[0] /= totalWeight;
            }

            this.backfitData(data, classProbs, totalWeight);
        }

        public Pair<Integer, Boolean> pizdunstvo(Instance instance) throws Exception {
            if (this.m_Attribute > -1) {
                Tree succ, another;
                if (RandomTreeWithExam.this.m_Info.attribute(this.m_Attribute).isNominal()) {
                    succ = this.m_Successors[(int) instance.value(this.m_Attribute)];
                    another = this.m_Successors[1-(int) instance.value(this.m_Attribute)];
                } else if (instance.value(this.m_Attribute) < this.m_SplitPoint) {
                    succ = this.m_Successors[0];
                    another = this.m_Successors[1];
                } else {
                    succ = this.m_Successors[1];
                    another = this.m_Successors[0];
                }
                Pair<Integer, Boolean> res = succ.pizdunstvo(instance);
                if (res!=null) return res;
                double[] sd = succ.distributionForInstance(instance);
                boolean correct = (sd[1]>sd[0])==(instance.value(instance.classIndex())>0.5);
                double[] ad = another.distributionForInstance(instance);
                boolean anotherValue = (ad[1]>ad[0])==(instance.value(instance.classIndex())>0.5);
                if (correct==anotherValue)
                    return null;
                if (correct)
                    return new Pair<>(m_Attribute,false);
                else
                    return new Pair<>(m_Attribute,true);

            }

            return null;
        }

        public void collectAttributes(Instance instance, ArrayList<Integer> to)  {
            if (this.m_Attribute > -1) {
                Tree succ;
                if (RandomTreeWithExam.this.m_Info.attribute(this.m_Attribute).isNominal()) {
                    succ = this.m_Successors[(int) instance.value(this.m_Attribute)];
                } else if (instance.value(this.m_Attribute) < this.m_SplitPoint) {
                    succ = this.m_Successors[0];
                } else {
                    succ = this.m_Successors[1];
                }
                to.add(m_Attribute);
                succ.collectAttributes(instance,to);
            }
        }

        public Tree getLeaf(Instance instance) {
            if (this.m_Attribute > -1) {
                Tree succ;
                if (RandomTreeWithExam.this.m_Info.attribute(this.m_Attribute).isNominal()) {
                    succ = this.m_Successors[(int) instance.value(this.m_Attribute)];
                } else if (instance.value(this.m_Attribute) < this.m_SplitPoint) {
                    succ = this.m_Successors[0];
                } else {
                    succ = this.m_Successors[1];
                }
                return succ.getLeaf(instance);
            }
            return this;

        }



        public double[] distributionForInstance(Instance instance)  {
            double[] returnedDist = null;
            if (this.m_Attribute > -1) {
                if (instance.isMissing(this.m_Attribute)) {
                    returnedDist = new double[RandomTreeWithExam.this.m_Info.numClasses()];

                    for(int i = 0; i < this.m_Successors.length; ++i) {
                        double[] help = this.m_Successors[i].distributionForInstance(instance);
                        if (help != null) {
                            for(int j = 0; j < help.length; ++j) {
                                returnedDist[j] += this.m_Prop[i] * help[j];
                            }
                        }
                    }
                } else if (RandomTreeWithExam.this.m_Info.attribute(this.m_Attribute).isNominal()) {
                    returnedDist = this.m_Successors[(int)instance.value(this.m_Attribute)].distributionForInstance(instance);
                } else if (instance.value(this.m_Attribute) < this.m_SplitPoint) {
                    returnedDist = this.m_Successors[0].distributionForInstance(instance);
                } else {
                    returnedDist = this.m_Successors[1].distributionForInstance(instance);
                }
            }

            if (this.m_Attribute != -1 && returnedDist != null) {
                return returnedDist;
            } else {
                double[] result;
                if (this.m_ClassDistribution == null) {
                    if (RandomTreeWithExam.this.getAllowUnclassifiedInstances()) {
                        result = new double[RandomTreeWithExam.this.m_Info.numClasses()];
                        if (RandomTreeWithExam.this.m_Info.classAttribute().isNumeric()) {
                            result[0] = Utils.missingValue();
                        }

                        return result;
                    } else {
                        return null;
                    }
                } else {
                    result = (double[])this.m_ClassDistribution.clone();
                    if (RandomTreeWithExam.this.m_Info.classAttribute().isNominal()) {
                        Utils.normalize(result);
                    }

                    return result;
                }
            }
        }

        public int toGraph(StringBuffer text, int num) throws Exception {
            int maxIndex = Utils.maxIndex(this.m_ClassDistribution);
            String classValue = RandomTreeWithExam.this.m_Info.classAttribute().isNominal() ? RandomTreeWithExam.this.m_Info.classAttribute().value(maxIndex) : Utils.doubleToString(this.m_ClassDistribution[0], RandomTreeWithExam.this.getNumDecimalPlaces());
            ++num;
            if (this.m_Attribute == -1) {
                text.append("N" + Integer.toHexString(this.hashCode()) + " [label=\"" + num + ": " + classValue + "\"shape=box]\n");
            } else {
                text.append("N" + Integer.toHexString(this.hashCode()) + " [label=\"" + num + ": " + classValue + "\"]\n");

                for(int i = 0; i < this.m_Successors.length; ++i) {
                    text.append("N" + Integer.toHexString(this.hashCode()) + "->N" + Integer.toHexString(this.m_Successors[i].hashCode()) + " [label=\"" + RandomTreeWithExam.this.m_Info.attribute(this.m_Attribute).name());
                    if (RandomTreeWithExam.this.m_Info.attribute(this.m_Attribute).isNumeric()) {
                        if (i == 0) {
                            text.append(" < " + Utils.doubleToString(this.m_SplitPoint, RandomTreeWithExam.this.getNumDecimalPlaces()));
                        } else {
                            text.append(" >= " + Utils.doubleToString(this.m_SplitPoint, RandomTreeWithExam.this.getNumDecimalPlaces()));
                        }
                    } else {
                        text.append(" = " + RandomTreeWithExam.this.m_Info.attribute(this.m_Attribute).value(i));
                    }

                    text.append("\"]\n");
                    num = this.m_Successors[i].toGraph(text, num);
                }
            }

            return num;
        }

        protected String leafString() throws Exception {
            double sum = 0.0D;
            double maxCount = 0.0D;
            int maxIndex = 0;
            double classMean = 0.0D;
            double avgError = 0.0D;
            if (this.m_ClassDistribution != null) {
                if (RandomTreeWithExam.this.m_Info.classAttribute().isNominal()) {
                    sum = Utils.sum(this.m_ClassDistribution);
                    maxIndex = Utils.maxIndex(this.m_ClassDistribution);
                    maxCount = this.m_ClassDistribution[maxIndex];
                } else {
                    classMean = this.m_ClassDistribution[0];
                    if (this.m_Distribution[1] > 0.0D) {
                        avgError = this.m_Distribution[0] / this.m_Distribution[1];
                    }
                }
            }

            return RandomTreeWithExam.this.m_Info.classAttribute().isNumeric() ? " : " + Utils.doubleToString(classMean, RandomTreeWithExam.this.getNumDecimalPlaces()) + " (" + Utils.doubleToString(this.m_Distribution[1], RandomTreeWithExam.this.getNumDecimalPlaces()) + "/" + Utils.doubleToString(avgError, RandomTreeWithExam.this.getNumDecimalPlaces()) + ")" : " : " + RandomTreeWithExam.this.m_Info.classAttribute().value(maxIndex) + " (" + Utils.doubleToString(sum, RandomTreeWithExam.this.getNumDecimalPlaces()) + "/" + Utils.doubleToString(sum - maxCount, RandomTreeWithExam.this.getNumDecimalPlaces()) + ")";
        }

        protected String toString(int level) {
            try {
                StringBuffer text = new StringBuffer();
                if (this.m_Attribute == -1) {
                    return this.leafString();
                } else {
                    int i;
                    if (RandomTreeWithExam.this.m_Info.attribute(this.m_Attribute).isNominal()) {
                        for(i = 0; i < this.m_Successors.length; ++i) {
                            text.append("\n");

                            for(int j = 0; j < level; ++j) {
                                text.append("|   ");
                            }

                            text.append(RandomTreeWithExam.this.m_Info.attribute(this.m_Attribute).name() + " = " + RandomTreeWithExam.this.m_Info.attribute(this.m_Attribute).value(i));
                            text.append(this.m_Successors[i].toString(level + 1));
                        }
                    } else {
                        text.append("\n");

                        for(i = 0; i < level; ++i) {
                            text.append("|   ");
                        }

                        text.append(RandomTreeWithExam.this.m_Info.attribute(this.m_Attribute).name() + " < " + Utils.doubleToString(this.m_SplitPoint, RandomTreeWithExam.this.getNumDecimalPlaces()));
                        text.append(this.m_Successors[0].toString(level + 1));
                        text.append("\n");

                        for(i = 0; i < level; ++i) {
                            text.append("|   ");
                        }

                        text.append(RandomTreeWithExam.this.m_Info.attribute(this.m_Attribute).name() + " >= " + Utils.doubleToString(this.m_SplitPoint, RandomTreeWithExam.this.getNumDecimalPlaces()));
                        text.append(this.m_Successors[1].toString(level + 1));
                    }

                    return text.toString();
                }
            } catch (Exception var5) {
                var5.printStackTrace();
                return "RandomTreeWithExam: tree can't be printed";
            }
        }

        protected void backfitData(Instances data, double[] classProbs, double totalWeight) throws Exception {
            if (data.numInstances() == 0) {
                this.m_Attribute = -1;
                this.m_ClassDistribution = null;
                if (data.classAttribute().isNumeric()) {
                    this.m_Distribution = new double[2];
                }

                this.m_Prop = null;
            } else {
                double priorVar = 0.0D;
                if (data.classAttribute().isNumeric()) {
                    double totalSum = 0.0D;
                    double totalSumSquared = 0.0D;
                    double totalSumOfWeights = 0.0D;

                    for(int i = 0; i < data.numInstances(); ++i) {
                        Instance inst = data.instance(i);
                        totalSum += inst.classValue() * inst.weight();
                        totalSumSquared += inst.classValue() * inst.classValue() * inst.weight();
                        totalSumOfWeights += inst.weight();
                    }

                    priorVar = RandomTreeWithExam.singleVariance(totalSum, totalSumSquared, totalSumOfWeights);
                }

                this.m_ClassDistribution = (double[])classProbs.clone();
                if (this.m_Attribute > -1) {
                    this.m_Prop = new double[this.m_Successors.length];

                    int var10001;
                    for(int ix = 0; ix < data.numInstances(); ++ix) {
                        Instance instx = data.instance(ix);
                        if (!instx.isMissing(this.m_Attribute)) {
                            double[] var10000;
                            if (data.attribute(this.m_Attribute).isNominal()) {
                                var10000 = this.m_Prop;
                                var10001 = (int)instx.value(this.m_Attribute);
                                var10000[var10001] += instx.weight();
                            } else {
                                var10000 = this.m_Prop;
                                var10001 = instx.value(this.m_Attribute) < this.m_SplitPoint ? 0 : 1;
                                var10000[var10001] += instx.weight();
                            }
                        }
                    }

                    if (Utils.sum(this.m_Prop) <= 0.0D) {
                        this.m_Attribute = -1;
                        this.m_Prop = null;
                        if (data.classAttribute().isNumeric()) {
                            this.m_Distribution = new double[2];
                            this.m_Distribution[0] = priorVar;
                            this.m_Distribution[1] = totalWeight;
                        }

                        return;
                    }

                    Utils.normalize(this.m_Prop);
                    Instances[] subsets = this.splitData(data);

                    int ixx;
                    for(ixx = 0; ixx < subsets.length; ++ixx) {
                        double[] dist = new double[data.numClasses()];
                        double sumOfWeights = 0.0D;

                        for(int j = 0; j < subsets[ixx].numInstances(); ++j) {
                            if (data.classAttribute().isNominal()) {
                                var10001 = (int)subsets[ixx].instance(j).classValue();
                                dist[var10001] += subsets[ixx].instance(j).weight();
                            } else {
                                dist[0] += subsets[ixx].instance(j).classValue() * subsets[ixx].instance(j).weight();
                                sumOfWeights += subsets[ixx].instance(j).weight();
                            }
                        }

                        if (sumOfWeights > 0.0D) {
                            dist[0] /= sumOfWeights;
                        }

                        this.m_Successors[ixx].backfitData(subsets[ixx], dist, totalWeight);
                    }

                    if (RandomTreeWithExam.this.getAllowUnclassifiedInstances()) {
                        this.m_ClassDistribution = null;
                        return;
                    }

                    for(ixx = 0; ixx < subsets.length; ++ixx) {
                        if (this.m_Successors[ixx].m_ClassDistribution == null) {
                            return;
                        }
                    }

                    this.m_ClassDistribution = null;
                }

            }
        }

        protected void buildTree(Instances data, double[] classProbs, int[] attIndicesWindow, double totalWeight, Random random, int depth, double minVariance) throws Exception {
            if (data.numInstances() == 0) {
                this.m_Attribute = -1;
                this.m_ClassDistribution = null;
                this.m_Prop = null;
                if (data.classAttribute().isNumeric()) {
                    this.m_Distribution = new double[2];
                }

            } else {
                double priorVar = 0.0D;
                double val;
                double split;
                int bestIndex;
                if (data.classAttribute().isNumeric()) {
                    val = 0.0D;
                    split = 0.0D;
                    double totalSumOfWeights = 0.0D;

                    for(bestIndex = 0; bestIndex < data.numInstances(); ++bestIndex) {
                        Instance inst = data.instance(bestIndex);
                        val += inst.classValue() * inst.weight();
                        split += inst.classValue() * inst.classValue() * inst.weight();
                        totalSumOfWeights += inst.weight();
                    }

                    priorVar = RandomTreeWithExam.singleVariance(val, split, totalSumOfWeights);
                }

                if (data.classAttribute().isNominal()) {
                    totalWeight = Utils.sum(classProbs);
                }

                if (totalWeight < 2.0D * RandomTreeWithExam.this.m_MinNum || data.classAttribute().isNominal() && Utils.eq(classProbs[Utils.maxIndex(classProbs)], Utils.sum(classProbs)) || data.classAttribute().isNumeric() && priorVar / totalWeight < minVariance || RandomTreeWithExam.this.getMaxDepth() > 0 && depth >= RandomTreeWithExam.this.getMaxDepth()) {
                    this.m_Attribute = -1;
                    this.m_ClassDistribution = (double[])classProbs.clone();
                    if (data.classAttribute().isNumeric()) {
                        this.m_Distribution = new double[2];
                        this.m_Distribution[0] = priorVar;
                        this.m_Distribution[1] = totalWeight;
                    }

                    this.m_Prop = null;
                } else {
                    val = -1.7976931348623157E308D;
                    split = -1.7976931348623157E308D;
                    double[][] bestDists = (double[][])null;
                    double[] bestProps = null;
                    bestIndex = 0;
                    double[][] props = new double[1][0];
                    double[][][] dists = new double[1][0][0];
                    double[][] totalSubsetWeights = new double[data.numAttributes()][0];
                    int attIndex;
                    int windowSize = attIndicesWindow.length;
                    int k = RandomTreeWithExam.this.m_KValue;
                    boolean gainFound = false;
                    double[] tempNumericVals = new double[data.numAttributes()];

                    while(windowSize > 0 && (k-- > 0 || !gainFound)) {
                        int chosenIndex = random.nextInt(windowSize);
                        int attIndexx = attIndicesWindow[chosenIndex];
                        attIndicesWindow[chosenIndex] = attIndicesWindow[windowSize - 1];
                        attIndicesWindow[windowSize - 1] = attIndexx;
                        --windowSize;
                        double currSplit = data.classAttribute().isNominal() ? this.distribution(props, dists, attIndexx, data) : this.numericDistribution(props, dists, attIndexx, totalSubsetWeights, data, tempNumericVals);
                        double currVal = data.classAttribute().isNominal() ? this.gain(dists[0], this.priorVal(dists[0])) : tempNumericVals[attIndexx];
                        if (Utils.gr(currVal, 0.0D)) {
                            gainFound = true;
                        }

                        if (currVal > val || !RandomTreeWithExam.this.getBreakTiesRandomly() && currVal == val && attIndexx < bestIndex) {
                            val = currVal;
                            bestIndex = attIndexx;
                            split = currSplit;
                            bestProps = props[0];
                            bestDists = dists[0];
                        }
                    }

                    this.m_Attribute = bestIndex;
                    if (Utils.gr(val, 0.0D)) {
                        if (RandomTreeWithExam.this.m_computeImpurityDecreases) {
                            RandomTreeWithExam.this.m_impurityDecreasees[this.m_Attribute][0] += val;
                            ++RandomTreeWithExam.this.m_impurityDecreasees[this.m_Attribute][1];
                        }

                        this.m_SplitPoint = split;
                        this.m_Prop = bestProps;
                        Instances[] subsets = this.splitData(data);
                        this.m_Successors = new RandomTreeWithExam.Tree[bestDists.length];
                        double[] attTotalSubsetWeights = totalSubsetWeights[bestIndex];

                        for(int i = 0; i < bestDists.length; ++i) {
                            this.m_Successors[i] = RandomTreeWithExam.this.new Tree();
                            this.m_Successors[i].buildTree(subsets[i], bestDists[i], attIndicesWindow, data.classAttribute().isNominal() ? 0.0D : attTotalSubsetWeights[i], random, depth + 1, minVariance);
                        }

                        boolean emptySuccessor = false;

                        for(int ix = 0; ix < subsets.length; ++ix) {
                            if (this.m_Successors[ix].m_ClassDistribution == null) {
                                emptySuccessor = true;
                                break;
                            }
                        }

                        if (emptySuccessor) {
                            this.m_ClassDistribution = (double[])classProbs.clone();
                        }
                    } else {
                        this.m_Attribute = -1;
                        this.m_ClassDistribution = (double[])classProbs.clone();
                        if (data.classAttribute().isNumeric()) {
                            this.m_Distribution = new double[2];
                            this.m_Distribution[0] = priorVar;
                            this.m_Distribution[1] = totalWeight;
                        }
                    }

                }
            }
        }

        public int numNodes() {
            if (this.m_Attribute == -1) {
                return 1;
            } else {
                int size = 1;
                RandomTreeWithExam.Tree[] var2 = this.m_Successors;
                int var3 = var2.length;

                for(int var4 = 0; var4 < var3; ++var4) {
                    RandomTreeWithExam.Tree m_Successor = var2[var4];
                    size += m_Successor.numNodes();
                }

                return size;
            }
        }

        protected Instances[] splitData(Instances data) throws Exception {
            Instances[] subsets = new Instances[this.m_Prop.length];

            int i;
            for(i = 0; i < this.m_Prop.length; ++i) {
                subsets[i] = new Instances(data, data.numInstances());
            }

            for(i = 0; i < data.numInstances(); ++i) {
                Instance inst = data.instance(i);
                if (inst.isMissing(this.m_Attribute)) {
                    for(int k = 0; k < this.m_Prop.length; ++k) {
                        if (this.m_Prop[k] > 0.0D) {
                            Instance copy = (Instance)inst.copy();
                            copy.setWeight(this.m_Prop[k] * inst.weight());
                            subsets[k].add(copy);
                        }
                    }
                } else if (data.attribute(this.m_Attribute).isNominal()) {
                    subsets[(int)inst.value(this.m_Attribute)].add(inst);
                } else {
                    if (!data.attribute(this.m_Attribute).isNumeric()) {
                        throw new IllegalArgumentException("Unknown attribute type");
                    }

                    subsets[inst.value(this.m_Attribute) < this.m_SplitPoint ? 0 : 1].add(inst);
                }
            }

            for(i = 0; i < this.m_Prop.length; ++i) {
                subsets[i].compactify();
            }

            return subsets;
        }

        protected double numericDistribution(double[][] props, double[][][] dists, int att, double[][] subsetWeights, Instances data, double[] vals) throws Exception {
            double splitPoint = 0.0D / 0.0;
            Attribute attribute = data.attribute(att);
            double[][] dist = (double[][])null;
            double[] sums = null;
            double[] sumSquared = null;
            double[] sumOfWeights = null;
            double totalSum = 0.0D;
            double totalSumSquared = 0.0D;
            double totalSumOfWeights = 0.0D;
            int indexOfFirstMissingValue = data.numInstances();
            int jxxx;
            double[] sumsx;
            double[] sumSquaredx;
            double[] sumOfWeightsx;
            if (attribute.isNominal()) {
                sumsx = new double[attribute.numValues()];
                sumSquaredx = new double[attribute.numValues()];
                sumOfWeightsx = new double[attribute.numValues()];

                for(int i = 0; i < data.numInstances(); ++i) {
                    Instance inst = data.instance(i);
                    if (inst.isMissing(att)) {
                        if (indexOfFirstMissingValue == data.numInstances()) {
                            indexOfFirstMissingValue = i;
                        }
                    } else {
                        jxxx = (int)inst.value(att);
                        sumsx[jxxx] += inst.classValue() * inst.weight();
                        sumSquaredx[jxxx] += inst.classValue() * inst.classValue() * inst.weight();
                        sumOfWeightsx[jxxx] += inst.weight();
                    }
                }

                totalSum = Utils.sum(sumsx);
                totalSumSquared = Utils.sum(sumSquaredx);
                totalSumOfWeights = Utils.sum(sumOfWeightsx);
            } else {
                sumsx = new double[2];
                sumSquaredx = new double[2];
                sumOfWeightsx = new double[2];
                double[] currSums = new double[2];
                double[] currSumSquared = new double[2];
                double[] currSumOfWeights = new double[2];
                data.sort(att);

                for(int jx = 0; jx < data.numInstances(); ++jx) {
                    Instance instx = data.instance(jx);
                    if (instx.isMissing(att)) {
                        indexOfFirstMissingValue = jx;
                        break;
                    }

                    currSums[1] += instx.classValue() * instx.weight();
                    currSumSquared[1] += instx.classValue() * instx.classValue() * instx.weight();
                    currSumOfWeights[1] += instx.weight();
                }

                totalSum = currSums[1];
                totalSumSquared = currSumSquared[1];
                totalSumOfWeights = currSumOfWeights[1];
                sumsx[1] = currSums[1];
                sumSquaredx[1] = currSumSquared[1];
                sumOfWeightsx[1] = currSumOfWeights[1];
                double currSplit = data.instance(0).value(att);
                double bestVal = 1.7976931348623157E308D;

                for(int ix = 0; ix < indexOfFirstMissingValue; ++ix) {
                    Instance instxx = data.instance(ix);
                    if (instxx.value(att) > currSplit) {
                        double currVal = RandomTreeWithExam.variance(currSums, currSumSquared, currSumOfWeights);
                        if (currVal < bestVal) {
                            bestVal = currVal;
                            splitPoint = (instxx.value(att) + currSplit) / 2.0D;
                            if (splitPoint <= currSplit) {
                                splitPoint = instxx.value(att);
                            }

                            for(int jxx = 0; jxx < 2; ++jxx) {
                                sumsx[jxx] = currSums[jxx];
                                sumSquaredx[jxx] = currSumSquared[jxx];
                                sumOfWeightsx[jxx] = currSumOfWeights[jxx];
                            }
                        }
                    }

                    currSplit = instxx.value(att);
                    double classVal = instxx.classValue() * instxx.weight();
                    double classValSquared = instxx.classValue() * classVal;
                    currSums[0] += classVal;
                    currSumSquared[0] += classValSquared;
                    currSumOfWeights[0] += instxx.weight();
                    currSums[1] -= classVal;
                    currSumSquared[1] -= classValSquared;
                    currSumOfWeights[1] -= instxx.weight();
                }
            }

            props[0] = new double[sumsx.length];

            for(jxxx = 0; jxxx < props[0].length; ++jxxx) {
                props[0][jxxx] = sumOfWeightsx[jxxx];
            }

            if (Utils.sum(props[0]) <= 0.0D) {
                for(jxxx = 0; jxxx < props[0].length; ++jxxx) {
                    props[0][jxxx] = 1.0D / (double)props[0].length;
                }
            } else {
                Utils.normalize(props[0]);
            }

            for(jxxx = indexOfFirstMissingValue; jxxx < data.numInstances(); ++jxxx) {
                Instance instxxx = data.instance(jxxx);

                for(int j = 0; j < sumsx.length; ++j) {
                    sumsx[j] += props[0][j] * instxxx.classValue() * instxxx.weight();
                    sumSquaredx[j] += props[0][j] * instxxx.classValue() * instxxx.classValue() * instxxx.weight();
                    sumOfWeightsx[j] += props[0][j] * instxxx.weight();
                }

                totalSum += instxxx.classValue() * instxxx.weight();
                totalSumSquared += instxxx.classValue() * instxxx.classValue() * instxxx.weight();
                totalSumOfWeights += instxxx.weight();
            }

            dist = new double[sumsx.length][data.numClasses()];

            for(jxxx = 0; jxxx < sumsx.length; ++jxxx) {
                if (sumOfWeightsx[jxxx] > 0.0D) {
                    dist[jxxx][0] = sumsx[jxxx] / sumOfWeightsx[jxxx];
                } else {
                    dist[jxxx][0] = totalSum / totalSumOfWeights;
                }
            }

            double priorVar = RandomTreeWithExam.singleVariance(totalSum, totalSumSquared, totalSumOfWeights);
            double var = RandomTreeWithExam.variance(sumsx, sumSquaredx, sumOfWeightsx);
            double gain = priorVar - var;
            subsetWeights[att] = sumOfWeightsx;
            dists[0] = dist;
            vals[att] = gain;
            return splitPoint;
        }

        protected double distribution(double[][] props, double[][][] dists, int att, Instances data) throws Exception {
            double splitPoint = 0.0D / 0.0;
            Attribute attribute = data.attribute(att);
            double[][] dist = (double[][])null;
            int indexOfFirstMissingValue = data.numInstances();
            int i;
            Instance inst;
            double[] var10000;
            int var10001;
            if (attribute.isNominal()) {
                dist = new double[attribute.numValues()][data.numClasses()];

                for(i = 0; i < data.numInstances(); ++i) {
                    inst = data.instance(i);
                    if (inst.isMissing(att)) {
                        if (indexOfFirstMissingValue == data.numInstances()) {
                            indexOfFirstMissingValue = i;
                        }
                    } else {
                        var10000 = dist[(int)inst.value(att)];
                        var10001 = (int)inst.classValue();
                        var10000[var10001] += inst.weight();
                    }
                }
            } else {
                double[][] currDist = new double[2][data.numClasses()];
                dist = new double[2][data.numClasses()];
                data.sort(att);

                for(int jx = 0; jx < data.numInstances(); ++jx) {
                    Instance instx = data.instance(jx);
                    if (instx.isMissing(att)) {
                        indexOfFirstMissingValue = jx;
                        break;
                    }

                    var10000 = currDist[1];
                    var10001 = (int)instx.classValue();
                    var10000[var10001] += instx.weight();
                }

                double priorVal = this.priorVal(currDist);

                for(int jxxx = 0; jxxx < currDist.length; ++jxxx) {
                    System.arraycopy(currDist[jxxx], 0, dist[jxxx], 0, dist[jxxx].length);
                }

                double currSplit = data.instance(0).value(att);
                double bestVal = -1.7976931348623157E308D;

                for(int ix = 0; ix < indexOfFirstMissingValue; ++ix) {
                    Instance instxx = data.instance(ix);
                    double attVal = instxx.value(att);
                    int j;
                    if (attVal > currSplit) {
                        double currVal = this.gain(currDist, priorVal);
                        if (currVal > bestVal) {
                            bestVal = currVal;
                            splitPoint = (attVal + currSplit) / 2.0D;
                            if (splitPoint <= currSplit) {
                                splitPoint = attVal;
                            }

                            for(j = 0; j < currDist.length; ++j) {
                                System.arraycopy(currDist[j], 0, dist[j], 0, dist[j].length);
                            }
                        }

                        currSplit = attVal;
                    }

                    j = (int)instxx.classValue();
                    currDist[0][j] += instxx.weight();
                    currDist[1][j] -= instxx.weight();
                }
            }

            props[0] = new double[dist.length];

            for(i = 0; i < props[0].length; ++i) {
                props[0][i] = Utils.sum(dist[i]);
            }

            if (Utils.eq(Utils.sum(props[0]), 0.0D)) {
                for(i = 0; i < props[0].length; ++i) {
                    props[0][i] = 1.0D / (double)props[0].length;
                }
            } else {
                Utils.normalize(props[0]);
            }

            for(i = indexOfFirstMissingValue; i < data.numInstances(); ++i) {
                inst = data.instance(i);
                int jxx;
                if (attribute.isNominal()) {
                    if (inst.isMissing(att)) {
                        for(jxx = 0; jxx < dist.length; ++jxx) {
                            var10000 = dist[jxx];
                            var10001 = (int)inst.classValue();
                            var10000[var10001] += props[0][jxx] * inst.weight();
                        }
                    }
                } else {
                    for(jxx = 0; jxx < dist.length; ++jxx) {
                        var10000 = dist[jxx];
                        var10001 = (int)inst.classValue();
                        var10000[var10001] += props[0][jxx] * inst.weight();
                    }
                }
            }

            dists[0] = dist;
            return splitPoint;
        }

        protected double priorVal(double[][] dist) {
            return ContingencyTables.entropyOverColumns(dist);
        }

        protected double gain(double[][] dist, double priorVal) {
            return priorVal - ContingencyTables.entropyConditionedOnRows(dist);
        }

        public String getRevision() {
            return RevisionUtils.extract("$Revision: 13865 $");
        }

        protected int toGraph(StringBuffer text, int num, RandomTreeWithExam.Tree parent) throws Exception {
            ++num;
            if (this.m_Attribute == -1) {
                text.append("N" + Integer.toHexString(this.hashCode()) + " [label=\"" + num + Utils.backQuoteChars(this.leafString()) + "\" shape=box]\n");
            } else {
                text.append("N" + Integer.toHexString(this.hashCode()) + " [label=\"" + num + ": " + Utils.backQuoteChars(RandomTreeWithExam.this.m_Info.attribute(this.m_Attribute).name()) + "\"]\n");

                for(int i = 0; i < this.m_Successors.length; ++i) {
                    text.append("N" + Integer.toHexString(this.hashCode()) + "->N" + Integer.toHexString(this.m_Successors[i].hashCode()) + " [label=\"");
                    if (RandomTreeWithExam.this.m_Info.attribute(this.m_Attribute).isNumeric()) {
                        if (i == 0) {
                            text.append(" < " + Utils.doubleToString(this.m_SplitPoint, RandomTreeWithExam.this.getNumDecimalPlaces()));
                        } else {
                            text.append(" >= " + Utils.doubleToString(this.m_SplitPoint, RandomTreeWithExam.this.getNumDecimalPlaces()));
                        }
                    } else {
                        text.append(" = " + Utils.backQuoteChars(RandomTreeWithExam.this.m_Info.attribute(this.m_Attribute).value(i)));
                    }

                    text.append("\"]\n");
                    num = this.m_Successors[i].toGraph(text, num, this);
                }
            }

            return num;
        }

        public void collectAttributes(HashSet<Integer> res) {
            if (m_Attribute>=0)
                res.add(m_Attribute);
            if (m_Successors!=null)
                for(int i = 0; i < m_Successors.length; ++i)
                    if (m_Successors[i]!=null) m_Successors[i].collectAttributes(res);

        }

        public void findGoodBranches(double minWeight, PriorityQueue<Branch> collect, int count, int maxClass, Branch current) {
            if (m_ClassDistribution!=null && m_ClassDistribution[0]+m_ClassDistribution[1]>minWeight && m_ClassDistribution[maxClass]>m_ClassDistribution[1-maxClass]) {
                double p = 1-Math.min(m_ClassDistribution[0],m_ClassDistribution[1])/(m_ClassDistribution[0]+m_ClassDistribution[1]);
                collect.add(new Branch(p,m_ClassDistribution[0],m_ClassDistribution[1],current));
                if (collect.size()>=count)
                    collect.poll();
            }
            if (m_Successors!=null)
                for(int i = 0; i < m_Successors.length; ++i)
                    if (m_Successors[i]!=null) {
                        current.splits.add(new Condition(m_Attribute, i, m_SplitPoint));
                        m_Successors[i].findGoodBranches(minWeight,collect, count, maxClass, current);
                        current.splits.remove(current.splits.size()-1);
                    }

        }


    }
    public static class Branch implements Comparable{
        public double p;
        public double d0,d1;
        public double tested_d0,tested_d1;
        public double test_w0, test_w1;
        public ArrayList<Condition> splits;

        public Instances set;
        public boolean penaltied = false;

        public Branch(double p, double d0, double d1, Branch from){
            this.p = p;
            this.d0 = d0;
            this.d1 = d1;
            splits = (ArrayList<Condition>) from.splits.clone();
            set = from.set;
        }

        public Branch(Instances set) {
            this.set = set;
            splits = new ArrayList<>();
        }

        private double coolness(){
            return p;
        }

        @Override
        public int compareTo(@NotNull Object o) {
            if (o instanceof Branch){
                Branch b = (Branch)o;
                return coolness() > b.coolness() ? 1 : -1;
            }
            return 0;
        }

        public boolean ok(){
            return (d0>d1 && tested_d0/test_w0*0.9>tested_d1/test_w1) || (d0<d1 && tested_d0/test_w0*1.1<tested_d1/test_w1);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (tested_d0+tested_d1>0){
                if (ok())
                    sb.append("+");
                else
                    sb.append("-");
            }
            sb.append(d0).append("/").append(d1);
            if (tested_d0+tested_d1>0){
                sb.append(" (");
                sb.append(tested_d0).append("/").append(tested_d1).append(")");
            }
            sb.append(":");
            for (Condition c : splits){
                sb.append(" ");
                Attribute att = set.attribute(c.attribute);
                sb.append(att.name());
                if (att.isNumeric()) {
                    sb.append(c.succIndex==0?"<":">=").append(c.split);
                } else {
                    sb.append("=").append(c.succIndex);
                }
            }
            return sb.toString();
        }

        public boolean checkSkipping(Instance inst, int skipAttr) {
            for (int i = 0;i<splits.size();i++){
                if (splits.get(i).attribute!=skipAttr && !splits.get(i).check(inst))
                    return false;
            }
            return true;
        }

        public boolean check(Instance ii, int canBeWrong) {
            for (Condition c : splits){
                if (!c.check(ii) && (canBeWrong--)<=0)
                    return false;
            }
            return true;

        }

        public int[] attributes(){
            return splits.stream().mapToInt(s->s.attribute).distinct().toArray();
        }

        public boolean test(Instances test) {
            test_w0 = CalcUtils.weightWithValue(test,test.classIndex(),0);
            test_w1 = CalcUtils.weightWithValue(test,test.classIndex(),1);
            tested_d0 = 0;
            tested_d1 = 0;
            for (int i = 0;i<test.size();i++){
                Instance ii = test.get(i);
                if (check(ii,0)) {
                    if (ii.value(ii.classIndex())==1)
                        tested_d1 += ii.weight();
                    else
                        tested_d0 += ii.weight();
                }
            }
            return ok();
        }

        public void collectPizdunstvo(double[] atts, double weight) {
            for (int i = 0;i<splits.size();i++)
                atts[splits.get(i).attribute]+=weight;
        }

        public int size() {
            return splits.size();
        }

        public void removeAttribute(Set<Integer> ignore) {
            splits.removeIf(s->ignore.contains(s.attribute));
        }
    }

    public static class Condition {
        public int attribute;
        public int succIndex;
        public double split;

        public Condition(int attribute, int index, double splitPoint) {
            this.attribute = attribute;
            succIndex = index;
            split = splitPoint;
        }

        public boolean check(Instance ii) {
            double v = ii.value(attribute);
            if (ii.attribute(attribute).isNominal())
                return succIndex==(int)v;
            if (succIndex==0)
                return v<split;
            else
                return v>=split;
        }
    }
}



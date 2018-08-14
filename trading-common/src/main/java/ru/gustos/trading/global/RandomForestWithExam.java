package ru.gustos.trading.global;

import kotlin.Pair;
import ru.gustos.trading.book.indicators.VecUtils;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.meta.Bagging;
import weka.core.*;
import weka.gui.ProgrammaticProperty;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import static weka.core.Capabilities.*;

public class RandomForestWithExam extends Bagging {
    static final long serialVersionUID = 1116839470751428698L;
    protected boolean m_computeAttributeImportance;

    protected int defaultNumberOfIterations() {
        return 100;
    }

    public RandomForestWithExam() {
        RandomTreeWithExam rTree = new RandomTreeWithExam();
        rTree.setDoNotCheckCapabilities(true);
        super.setClassifier(rTree);
        super.setRepresentCopiesUsingWeights(true);
        this.setNumIterations(this.defaultNumberOfIterations());
    }

    public Capabilities getCapabilities() {
        return (new RandomTreeWithExam()).getCapabilities();
    }

    protected String defaultClassifierString() {
        return "weka.classifiers.trees.RandomTreeWithExam";
    }

    protected String[] defaultClassifierOptions() {
        String[] args = new String[]{"-do-not-check-capabilities"};
        return args;
    }

    public String globalInfo() {
        return "Class for constructing a forest of random trees.\n\nFor more information see: \n\n" + this.getTechnicalInformation().toString();
    }

    public TechnicalInformation getTechnicalInformation() {
        TechnicalInformation result = new TechnicalInformation(TechnicalInformation.Type.ARTICLE);
        result.setValue(TechnicalInformation.Field.AUTHOR, "Leo Breiman");
        result.setValue(TechnicalInformation.Field.YEAR, "2001");
        result.setValue(TechnicalInformation.Field.TITLE, "Random Forests");
        result.setValue(TechnicalInformation.Field.JOURNAL, "Machine Learning");
        result.setValue(TechnicalInformation.Field.VOLUME, "45");
        result.setValue(TechnicalInformation.Field.NUMBER, "1");
        result.setValue(TechnicalInformation.Field.PAGES, "5-32");
        return result;
    }

    @ProgrammaticProperty
    public void setClassifier(Classifier newClassifier) {
        if (!(newClassifier instanceof RandomTreeWithExam)) {
            throw new IllegalArgumentException("RandomForest: Argument of setClassifier() must be a RandomTreeWithExam.");
        } else {
            super.setClassifier(newClassifier);
        }
    }

    @ProgrammaticProperty
    public void setRepresentCopiesUsingWeights(boolean representUsingWeights) {
        if (!representUsingWeights) {
            throw new IllegalArgumentException("RandomForest: Argument of setRepresentCopiesUsingWeights() must be true.");
        } else {
            super.setRepresentCopiesUsingWeights(representUsingWeights);
        }
    }

    public String numFeaturesTipText() {
        return ((RandomTreeWithExam) this.getClassifier()).KValueTipText();
    }

    public int getNumFeatures() {
        return ((RandomTreeWithExam) this.getClassifier()).getKValue();
    }

    public void setNumFeatures(int newNumFeatures) {
        ((RandomTreeWithExam) this.getClassifier()).setKValue(newNumFeatures);
    }

    public String computeAttributeImportanceTipText() {
        return "Compute attribute importance via mean impurity decrease";
    }

    public void setComputeAttributeImportance(boolean computeAttributeImportance) {
        this.m_computeAttributeImportance = computeAttributeImportance;
        ((RandomTreeWithExam) this.m_Classifier).setComputeImpurityDecreases(computeAttributeImportance);
    }

    public boolean getComputeAttributeImportance() {
        return this.m_computeAttributeImportance;
    }

    public String maxDepthTipText() {
        return ((RandomTreeWithExam) this.getClassifier()).maxDepthTipText();
    }

    public int getMaxDepth() {
        return ((RandomTreeWithExam) this.getClassifier()).getMaxDepth();
    }

    public void setMaxDepth(int value) {
        ((RandomTreeWithExam) this.getClassifier()).setMaxDepth(value);
    }

    public String breakTiesRandomlyTipText() {
        return ((RandomTreeWithExam) this.getClassifier()).breakTiesRandomlyTipText();
    }

    public boolean getBreakTiesRandomly() {
        return ((RandomTreeWithExam) this.getClassifier()).getBreakTiesRandomly();
    }

    public void setBreakTiesRandomly(boolean newBreakTiesRandomly) {
        ((RandomTreeWithExam) this.getClassifier()).setBreakTiesRandomly(newBreakTiesRandomly);
    }

    public void setDebug(boolean debug) {
        super.setDebug(debug);
        ((RandomTreeWithExam) this.getClassifier()).setDebug(debug);
    }

    public void setNumDecimalPlaces(int num) {
        super.setNumDecimalPlaces(num);
        ((RandomTreeWithExam) this.getClassifier()).setNumDecimalPlaces(num);
    }

    public void setBatchSize(String size) {
        super.setBatchSize(size);
        ((RandomTreeWithExam) this.getClassifier()).setBatchSize(size);
    }

    public void setSeed(int s) {
        super.setSeed(s);
        ((RandomTreeWithExam) this.getClassifier()).setSeed(s);
    }

    public String toString() {
        if (this.m_Classifiers == null) {
            return "RandomForest: No model built yet.";
        } else {
            StringBuilder buffer = new StringBuilder("RandomForest\n\n");
            buffer.append(super.toString());
            if (this.getComputeAttributeImportance()) {
                try {
                    double[] nodeCounts = new double[this.m_data.numAttributes()];
                    double[] impurityScores = this.computeAverageImpurityDecreasePerAttribute(nodeCounts);
                    int[] sortedIndices = Utils.sort(impurityScores);
                    buffer.append("\n\nAttribute importance based on average impurity decrease (and number of nodes using that attribute)\n\n");

                    for (int i = sortedIndices.length - 1; i >= 0; --i) {
                        int index = sortedIndices[i];
                        if (index != this.m_data.classIndex()) {
                            buffer.append(Utils.doubleToString(impurityScores[index], 10, this.getNumDecimalPlaces())).append(" (").append(Utils.doubleToString(nodeCounts[index], 6, 0)).append(")  ").append(this.m_data.attribute(index).name()).append("\n");
                        }
                    }
                } catch (WekaException var7) {
                    ;
                }
            }

            return buffer.toString();
        }
    }

    public double[][] computePizdunstvo(Instance inst) throws Exception {
        double[][] pizdunstvo = new double[2][this.m_data.numAttributes()];

        Classifier[] classifiers = this.m_Classifiers;

        for (int j = 0; j < classifiers.length; ++j) {
            RandomTreeWithExam c = (RandomTreeWithExam) classifiers[j];
            Pair<Integer, Boolean> pizd = c.computePizdunstvo(inst);
            if (pizd != null) {
                if (pizd.getSecond()) pizdunstvo[0][pizd.getFirst()]++;
                pizdunstvo[1][pizd.getFirst()]++;
            } else
                pizd = c.computePizdunstvo(inst);
        }

        return pizdunstvo;

    }

    public double[][] computePizdunstvo2(Instance inst) throws Exception {
        double[][] pizdunstvo = new double[2][this.m_data.numAttributes()];

        Classifier[] classifiers = this.m_Classifiers;

        for (int j = 0; j < classifiers.length; ++j) {
            RandomTreeWithExam c = (RandomTreeWithExam) classifiers[j];
            c.computePizdunstvo2(inst, pizdunstvo);
        }

        return pizdunstvo;

    }

    public double[] computeAverageImpurityDecreasePerAttribute(double[] nodeCounts) throws WekaException {
        if (this.m_Classifiers == null) {
            throw new WekaException("Classifier has not been built yet!");
        } else if (!this.getComputeAttributeImportance()) {
            throw new WekaException("Stats for attribute importance have not been collected!");
        } else {
            double[] impurityDecreases = new double[this.m_data.numAttributes()];
            if (nodeCounts == null) {
                nodeCounts = new double[this.m_data.numAttributes()];
            }

            Classifier[] var3 = this.m_Classifiers;
            int var4 = var3.length;

            for (int var5 = 0; var5 < var4; ++var5) {
                Classifier c = var3[var5];
                double[][] forClassifier = ((RandomTreeWithExam) c).getImpurityDecreases();

                for (int i = 0; i < this.m_data.numAttributes(); ++i) {
                    impurityDecreases[i] += forClassifier[i][0];
                    nodeCounts[i] += forClassifier[i][1];
                }
            }

            for (int i = 0; i < this.m_data.numAttributes(); ++i) {
                if (nodeCounts[i] > 0.0D) {
                    impurityDecreases[i] /= nodeCounts[i];
                }
            }

            return impurityDecreases;
        }
    }

    public Enumeration<Option> listOptions() {
        Vector<Option> newVector = new Vector();
        newVector.addElement(new Option("\tSize of each bag, as a percentage of the\n\ttraining set size. (default 100)", "P", 1, "-P"));
        newVector.addElement(new Option("\tCalculate the out of bag error.", "O", 0, "-O"));
        newVector.addElement(new Option("\tWhether to store out of bag predictions in internal evaluation object.", "store-out-of-bag-predictions", 0, "-store-out-of-bag-predictions"));
        newVector.addElement(new Option("\tWhether to output complexity-based statistics when out-of-bag evaluation is performed.", "output-out-of-bag-complexity-statistics", 0, "-output-out-of-bag-complexity-statistics"));
        newVector.addElement(new Option("\tPrint the individual classifiers in the output", "print", 0, "-print"));
        newVector.addElement(new Option("\tCompute and output attribute importance (mean impurity decrease method)", "attribute-importance", 0, "-attribute-importance"));
        newVector.addElement(new Option("\tNumber of iterations.\n\t(current value " + this.getNumIterations() + ")", "I", 1, "-I <num>"));
        newVector.addElement(new Option("\tNumber of execution slots.\n\t(default 1 - i.e. no parallelism)\n\t(use 0 to auto-detect number of cores)", "num-slots", 1, "-num-slots <num>"));
        List<Option> list = Collections.list(((OptionHandler) this.getClassifier()).listOptions());
        newVector.addAll(list);
        return newVector.elements();
    }

    public String[] getOptions() {
        Vector<String> result = new Vector();
        result.add("-P");
        result.add("" + this.getBagSizePercent());
        if (this.getCalcOutOfBag()) {
            result.add("-O");
        }

        if (this.getStoreOutOfBagPredictions()) {
            result.add("-store-out-of-bag-predictions");
        }

        if (this.getOutputOutOfBagComplexityStatistics()) {
            result.add("-output-out-of-bag-complexity-statistics");
        }

        if (this.getPrintClassifiers()) {
            result.add("-print");
        }

        if (this.getComputeAttributeImportance()) {
            result.add("-attribute-importance");
        }

        result.add("-I");
        result.add("" + this.getNumIterations());
        result.add("-num-slots");
        result.add("" + this.getNumExecutionSlots());
        if (this.getDoNotCheckCapabilities()) {
            result.add("-do-not-check-capabilities");
        }

        Vector<String> classifierOptions = new Vector();
        Collections.addAll(classifierOptions, ((OptionHandler) this.getClassifier()).getOptions());
        Option.deleteFlagString(classifierOptions, "-do-not-check-capabilities");
        result.addAll(classifierOptions);
        return (String[]) result.toArray(new String[result.size()]);
    }

    public void setOptions(String[] options) throws Exception {
        String bagSize = Utils.getOption('P', options);
        if (bagSize.length() != 0) {
            this.setBagSizePercent(Integer.parseInt(bagSize));
        } else {
            this.setBagSizePercent(100);
        }

        this.setCalcOutOfBag(Utils.getFlag('O', options));
        this.setStoreOutOfBagPredictions(Utils.getFlag("store-out-of-bag-predictions", options));
        this.setOutputOutOfBagComplexityStatistics(Utils.getFlag("output-out-of-bag-complexity-statistics", options));
        this.setPrintClassifiers(Utils.getFlag("print", options));
        this.setComputeAttributeImportance(Utils.getFlag("attribute-importance", options));
        String iterations = Utils.getOption('I', options);
        if (iterations.length() != 0) {
            this.setNumIterations(Integer.parseInt(iterations));
        } else {
            this.setNumIterations(this.defaultNumberOfIterations());
        }

        String numSlots = Utils.getOption("num-slots", options);
        if (numSlots.length() != 0) {
            this.setNumExecutionSlots(Integer.parseInt(numSlots));
        } else {
            this.setNumExecutionSlots(1);
        }

        RandomTreeWithExam classifier = (RandomTreeWithExam) AbstractClassifier.forName(this.defaultClassifierString(), options);
        classifier.setComputeImpurityDecreases(this.m_computeAttributeImportance);
        this.setDoNotCheckCapabilities(classifier.getDoNotCheckCapabilities());
        this.setSeed(classifier.getSeed());
        this.setDebug(classifier.getDebug());
        this.setNumDecimalPlaces(classifier.getNumDecimalPlaces());
        this.setBatchSize(classifier.getBatchSize());
        classifier.setDoNotCheckCapabilities(true);
        this.setClassifier(classifier);
        Utils.checkForRemainingOptions(options);
    }

    public String getRevision() {
        return RevisionUtils.extract("$Revision: 13295 $");
    }

    public static void main(String[] argv) {
        runClassifier(new RandomForestWithExam(), argv);
    }
}



package ru.gustos.trading.ml;

import weka.classifiers.trees.J48;
import weka.classifiers.trees.j48.C45Split;
import weka.classifiers.trees.j48.ClassifierSplitModel;
import weka.classifiers.trees.j48.ClassifierTree;
import weka.classifiers.trees.j48.NoSplit;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.HashSet;

public class J48AttributeFilter{

    private final int folds;
    private final double window;
    private boolean[] good;
    private int goodCount;

    public J48AttributeFilter(int folds, double window){
        this.folds = folds;
        this.window = window;
    }

    public void prepare(Instances set) throws Exception {
        int w = (int)(set.size()*window);
        good = new boolean[set.numAttributes()];
        good[set.classIndex()] = true;
        for (int i = 0;i<folds;i++){
            int from = (set.size()-w)*i/(folds-1);
            Instances t = new Instances(set, from, w);
            MyJ48 j = new MyJ48();
            j.buildClassifier(t);
            j.collect(good);
        }
        goodCount = 0;
        for (int i = 0;i<good.length;i++)
            if (good[i])
                goodCount++;
    }

    public Instances filter(Instances set) {
        ArrayList<Attribute> attributes = new ArrayList<>();
        for (int i = 0;i<set.numAttributes();i++)
            if (good[i])
                attributes.add(set.attribute(i));

        Instances filtered = new Instances("filtered", attributes, set.size());
        filtered.setClassIndex(attributes.size()-1);
        for (int i = 0;i<set.size();i++)
            filtered.add(filter(set.get(i)));
        return filtered;
    }

    public Instance filter(Instance set) {
        double g[] = new double[goodCount];
        int n = 0;
        for (int i = 0;i<set.numAttributes();i++)
            if (good[i])
                g[n++] = set.value(i);
        DenseInstance result = new DenseInstance(set.weight(), g);
        return result;
    }

    public boolean isGood(int index) {
        return good[index];
    }


    class MyJ48 extends J48{

        public void collect(boolean[] good){
            collect(m_root,good);
        }
        void collect(ClassifierTree tree, boolean[] good){
            ClassifierSplitModel model = tree.getLocalModel();
            if (model instanceof  C45Split) {
                good[((C45Split) model).attIndex()] = true;
                for (ClassifierTree s : tree.getSons())
                    collect(s, good);
            }
//            else if (!(model instanceof NoSplit)){
//                int k = 0;
//            }
        }
    }
}

package ru.gustos.trading.global;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class MomentDataHelper {
    Hashtable<String,Integer> map = new Hashtable<>();
    ArrayList<MetaData> metas = new ArrayList<>();


    public void register(String key) {
        register(key,false);
    }
    public void register(String key, boolean bool){
        if (map.contains(key))
            throw new NullPointerException();
        MetaData md = new MetaData();
        md.key = key;
        md.bool = bool;
        md.index = metas.size();
        md.future = key.startsWith("_");
        md.result = key.startsWith("@");
        if (Character.isDigit(key.charAt(0)))
            md.level = key.charAt(0)-'0';
//        System.out.println(key);
        map.put(key,metas.size());
        metas.add(md);
    }

    public int dataAttributes(int level){
        int cc = 0;
        for (int i = 0;i<metas.size();i++)
            if (metas.get(i).data(level))
                cc++;
        return cc;
    }

    public int futureAttributes(){
        int cc = 0;
        for (int i = 0;i<metas.size();i++)
            if (metas.get(i).future)
                cc++;
        return cc;
    }

    public int futureAttributePos(int futureAttribute){
        int cc = 0;
        for (int i = 0;i<metas.size();i++)
            if (metas.get(i).future) {
                if (cc==futureAttribute) return i;
                cc++;
            }
        return -1;

    }

    public double get(MomentData m, String key){
        return m.values[map.get(key)];
    }

    public void put(MomentData m, String key, double value){
        put(m,key,value,false);
    }
    public void put(MomentData m, String key, double value, boolean bool){
        if (!map.containsKey(key))
            register(key,bool);
        if (value==Double.NaN)
            System.out.println("putting nan to "+key);
        if (value==Double.POSITIVE_INFINITY)
            System.out.println("putting +Inf to "+key);
        if (value==Double.NEGATIVE_INFINITY)
            System.out.println("putting -Inf to "+key);
        m.values[map.get(key)] = value;
    }

    public void putResult(MomentData m, int futureAttribute, boolean result) {
        int pos = futureAttributePos(futureAttribute);
        MetaData data = metas.get(pos);
        String key = "@"+data.key.substring(1);
        put(m,key,result?1.0:0,true);
    }

    public int size() {
        return metas.size();
    }

    public ArrayList<Attribute> makeAttributes(int futureAttribute, int level) {
        ArrayList<Attribute> attributes = new ArrayList<>();
        for (MetaData meta : metas) {
            if (meta.data(level)) {
                if (meta.bool)
                    attributes.add(new Attribute(meta.key, Arrays.asList("false", "true")));
                else
                    attributes.add(new Attribute(meta.key));
            }
        }
        int i = futureAttributePos(futureAttribute);
        MetaData meta = metas.get(i);
        if (meta.bool)
            attributes.add(new Attribute(meta.key, Arrays.asList("false", "true")));
        else
            attributes.add(new Attribute(meta.key));
        return attributes;
    }

    public Instance makeInstance(MomentData data, int futureAttribute, int level){
        double[] vv = data.values;
        double[] v = new double[dataAttributes(level)+1];
        int p = 0;
        for (int j = 0;j<metas.size();j++)
            if (metas.get(j).data(level))
                v[p++] = vv[metas.get(j).index];
        v[p] = vv[metas.get(futureAttributePos(futureAttribute)).index];
        return new DenseInstance(data.weight,v);
    }

    public Instances makeEmptySet(int futureAttribute, int level){
        Instances set = new Instances("data", makeAttributes(futureAttribute, level), 10);
        set.setClassIndex(set.numAttributes()-1);
        return set;
    }
    public Instances makeSet(MomentDataProvider[] data, int from, int index, int futureAttribute, int level){
        Instances set = makeEmptySet(futureAttribute, level);
        for (int i = from;i<Math.min(index,data.length);i++) if (data[i]!=null && data[i].getMomentData().whenWillKnow<index)
            set.add(makeInstance(data[i].getMomentData(),futureAttribute, level));

        return set;
    }

    public Instances makeSet(List<? extends MomentDataProvider> data, int from, int index, long endtime, int futureAttribute, int level){
        Instances set = makeEmptySet(futureAttribute, level);
        for (int i = from;i<Math.min(index,data.size());i++) if (data.get(i)!=null && data.get(i).getMomentData().whenWillKnow<endtime)
            set.add(makeInstance(data.get(i).getMomentData(),futureAttribute, level));

        return set;
    }

    public boolean classify(MomentData mldata, Classifier classifier, int futureAttribute, int level) {
        Instance instance = makeInstance(mldata, futureAttribute, level);
        instance.setDataset(makeEmptySet(futureAttribute,level));
        try {
            double v = classifier.classifyInstance(instance);
            return v>0.5;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void printImpurity(Instances set1, double[] impurity, String prefix) {
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0;i<impurity.length;i++) {
            if (i!=0)
                sb.append(",");
            sb.append(set1.attribute(i).name()).append("=").append(String.format("%.3g", impurity[i]));
        }
        System.out.println(sb.toString());
    }
}

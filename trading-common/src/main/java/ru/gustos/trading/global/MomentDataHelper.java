package ru.gustos.trading.global;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.stream.Collectors;

import kotlin.Pair;
import org.apache.commons.io.FileUtils;
import ru.gustos.trading.ml.J48AttributeFilter;
import smile.classification.RandomForest;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class MomentDataHelper {

    public static HashSet<String> ignore = new HashSet<>();
    public static String ignoreString = "";

    Hashtable<String, Integer> map = new Hashtable<>();
    ArrayList<MetaData> metas = new ArrayList<>();
    public static double threshold = 0.5;

    public static void loadIgnore(String fileName) throws IOException {
        File ignoreFile = new File(fileName);
        if (ignoreFile.exists()) {
            ignoreString = FileUtils.readFileToString(ignoreFile);
            String[] ss = ignoreString.split("\n");
            ignore.addAll(Arrays.stream(ss[0].trim().split(",")).collect(Collectors.toList()));
        }

    }


    public void register(String key) {
        register(key, false);
    }

    public void register(String key, boolean bool) {
        if (ignore.contains(key)) return;
        if (map.contains(key))
            throw new NullPointerException();
        MetaData md = new MetaData();
        md.key = key;
        md.bool = bool;
        md.index = metas.size();
        md.future = key.startsWith("_");
        md.result = key.startsWith("@");
        if (Character.isDigit(key.charAt(0)))
            md.level = key.charAt(0) - '0';
//        System.out.println(key);
        map.put(key, metas.size());
        metas.add(md);
    }

    public int dataAttributes(HashSet<String> ignoreAttributes, J48AttributeFilter filter, int level) {
        int cc = 0;
        for (int i = 0; i < metas.size(); i++)
            if (metas.get(i).data(ignoreAttributes, filter, level))
                cc++;
        return cc;
    }

    public int futureAttributes() {
        int cc = 0;
        for (int i = 0; i < metas.size(); i++)
            if (metas.get(i).future)
                cc++;
        return cc;
    }

    public int futureAttributePos(int futureAttribute) {
        int cc = 0;
        for (int i = 0; i < metas.size(); i++)
            if (metas.get(i).future) {
                if (cc == futureAttribute) return i;
                cc++;
            }
        return -1;

    }

    public double get(MomentData m, String key) {
        return m.values[map.get(key)];
    }

    public double get(MomentData m, String key, double def) {
        if (!map.containsKey(key)) return def;
        return m.values[map.get(key)];
    }

    public void put(MomentData m, String key, double value) {
        put(m, key, value, false);
    }

    public void putLagged(MomentData m, String key, MomentData from, int lag) {
        double value = get(from, key);
        double valuen = get(m, key);
        put(m, key + "_lag" + lag, value, false);
//        put(m,key+"_delta"+lag,valuen-value,false);
    }

    public void putDelta(MomentData m, String key, MomentData from, int lag) {
        double value = get(from, key);
        double valuen = get(m, key);
        put(m, key + "_delta" + lag, valuen - value, false);
    }

    public void putLagged(MomentData to, MomentData from, int lag) {
        for (int i = 0; i < metas.size(); i++) {
            MetaData m = metas.get(i);
            if (m.data(null, null, 9) && m.key.indexOf('_') < 0) {
                putLagged(to, m.key, from, lag);
                putDelta(to, m.key, from, lag);
            }
        }
    }


    public void put(MomentData m, String key, double value, boolean bool) {
        if (ignore.contains(key)) return;
        if (!map.containsKey(key))
            register(key, bool);
        if (value == Double.NaN)
            System.out.println("putting nan to " + key);
        if (value == Double.POSITIVE_INFINITY)
            System.out.println("putting +Inf to " + key);
        if (value == Double.NEGATIVE_INFINITY)
            System.out.println("putting -Inf to " + key);
        m.values[map.get(key)] = value;
    }

    public void putResult(MomentData m, int futureAttribute, String logic, boolean result) {
        int pos = futureAttributePos(futureAttribute);
        MetaData data = metas.get(pos);
        String key = "@" + data.key.substring(1);
        if (logic != null)
            key += "|" + logic;
        put(m, key, result ? 1.0 : 0, true);
    }

    public int size() {
        return metas.size();
    }

    public ArrayList<Attribute> makeAttributes(HashSet<String> ignoreAttributes, J48AttributeFilter filter, int futureAttribute, int level) {
        ArrayList<Attribute> attributes = new ArrayList<>();

        for (MetaData meta : metas) {
            if (meta.data(ignoreAttributes, filter, level) && (filter == null || filter.isGood(meta.index))) {
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

    public Instance makeInstance(MomentData data, HashSet<String> ignoreAttributes, J48AttributeFilter filter, int futureAttribute, int level) {
        double[] vv = data.values;
        double[] v = new double[dataAttributes(ignoreAttributes, filter, level) + 1];
        int p = 0;
        for (int j = 0; j < metas.size(); j++)
            if (metas.get(j).data(ignoreAttributes, filter, level))
                v[p++] = vv[metas.get(j).index];
        v[p] = vv[metas.get(futureAttributePos(futureAttribute)).index];
        return new DenseInstance(data.weight, v);
    }

    public Instances makeEmptySet(HashSet<String> ignoreAttributes, J48AttributeFilter filter, int futureAttribute, int level) {
        Instances set = new Instances("data", makeAttributes(ignoreAttributes, filter, futureAttribute, level), 10);
        set.setClassIndex(set.numAttributes() - 1);
        return set;
    }

    public Instances makeSet(List<? extends MomentDataProvider> data, HashSet<String> ignoreAttributes, J48AttributeFilter filter, int from, int to, long endtime, int futureAttribute, int level) {
        return makeSet(data, ignoreAttributes, filter, from, to, endtime, futureAttribute, level, 0);
    }

    public Instances makeSet(List<? extends MomentDataProvider> data, HashSet<String> ignoreAttributes, J48AttributeFilter filter, int from, int to, long endtime, int futureAttribute, int level, double weightFrom) {
        Instances set = makeEmptySet(ignoreAttributes, filter, futureAttribute, level);
        for (int i = from; i < Math.min(to, data.size()); i++) {
            MomentDataProvider d = data.get(i);
            if (d != null) {
                MomentData md = d.getMomentData();
                if (!md.ignore && md.whenWillKnow < endtime && md.weight >= weightFrom)
                    set.add(makeInstance(md, ignoreAttributes, filter, futureAttribute, level));
            }
        }

        return set;
    }

    public Instance prepareInstance(MomentData mldata, HashSet<String> ignoreAttributes, J48AttributeFilter filter, int futureAttribute, int level) {
        Instance instance = makeInstance(mldata, ignoreAttributes, filter, futureAttribute, level);
        instance.setDataset(makeEmptySet(ignoreAttributes, filter, futureAttribute, level));

        return instance;
    }

    public boolean classify(MomentData mldata, HashSet<String> ignoreAttributes, J48AttributeFilter filter, Object classifier, int futureAttribute, int level) {
        Instance instance = prepareInstance(mldata, ignoreAttributes, filter, futureAttribute, level);
        if (classifier instanceof Classifier) {
            try {
                double v = ((Classifier) classifier).classifyInstance(instance);
                return v > threshold;
            } catch (Exception e) {
                e.printStackTrace();
                throw new NullPointerException("error classifying");
            }
        } else {
            RandomForest rf = (RandomForest) classifier;
            return rf.predict(CalcUtils.smileInstance(instance)) == 1;
        }
    }

    public void addAvgAndDisp(InstrumentData data, String name, int index, int cnt, boolean avg, boolean disp) {
        addAvgAndDisp(data, name, index, cnt, 0, avg, disp);
    }

    public void addAvgAndDisp(InstrumentData data, String name, int index, int cnt, int offset, boolean addavg, boolean adddisp) {
        double avg = 0;
        double sd = 0;
        int cc = 0;
        if (map.get(name) != null) {
            for (int i = index - offset; i > Math.max(-1, index - cnt - offset); i--) {
                avg += get(data.data.get(i), name);
                cc++;
            }
            avg /= cc;
            for (int i = index - offset; i > Math.max(-1, index - cnt - offset); i--) {
                double v = get(data.data.get(i), name);
                v = v - avg;
                sd += v * v;
            }
            sd = Math.sqrt(sd / cc);
        }

        if (name.startsWith("@")) name = name.substring(1);
        if (addavg)
            put(data.data.get(index), name + "_avg" + cnt, avg);
        if (adddisp)
            put(data.data.get(index), name + "_sd" + cnt, sd);
    }

    public void addAvgAndDispVolume(InstrumentData data, String name, int index, int cnt, int offset, boolean addavg, boolean adddisp) {
        double avg = 0;
        double sd = 0;
        int cc = 0;
        for (int i = index - offset; i > Math.max(-1, index - cnt - offset); i--) {
            float volume = data.bar(i).getVolume();
            avg += volume;
            cc++;
        }
        avg /= cc;
        for (int i = index - offset; i > Math.max(-1, index - cnt - offset); i--) {
            double v = data.bar(i).getVolume();
            v = v - avg;
            sd += v * v;
        }
        sd = Math.sqrt(sd / cc);

        if (addavg)
            put(data.data.get(index), name + "_avg" + cnt, avg);
        if (adddisp)
            put(data.data.get(index), name + "_sd" + cnt, sd);
    }

    public void save(ObjectOutputStream out, ArrayList<MomentData> data) throws IOException {
        out.writeObject(metas);
        out.writeObject(data);
    }

    public ArrayList<MomentData> load(ObjectInputStream in) throws IOException, ClassNotFoundException {
        metas = (ArrayList<MetaData>) in.readObject();
        for (MetaData m : metas)
            map.put(m.key,m.index);
        return (ArrayList<MomentData>)in.readObject();

    }
}

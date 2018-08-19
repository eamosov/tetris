package ru.gustos.trading.global;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TreePizdunstvo{
    public static TreePizdunstvo p = new TreePizdunstvo();

    HashMap<String,PizdStat> stat = new HashMap<>();

    public void add(String key, boolean real, boolean predicted){
        PizdStat p = stat.get(key);
        if (p==null){
            p = new PizdStat();
            stat.put(key,p);
        }
        p.count++;
        int ind = (real?2:0)+(predicted?1:0);
        p.cm[ind]++;
    }

    public void save(DataOutputStream out) throws IOException {
        out.writeInt(stat.size());
        for (String k : stat.keySet()){
            out.writeUTF(k);
            PizdStat p = stat.get(k);
            out.writeInt(p.count);
            out.writeInt(p.cm[0]);
            out.writeInt(p.cm[1]);
            out.writeInt(p.cm[2]);
            out.writeInt(p.cm[3]);
        }
    }

    public void load(DataInputStream in) throws IOException {
        int cc = in.readInt();
        while (cc-->0){
            String key = in.readUTF();
            PizdStat p = new PizdStat();
            stat.put(key,p);
            p.count = in.readInt();
            p.cm[0] = in.readInt();
            p.cm[1] = in.readInt();
            p.cm[2] = in.readInt();
            p.cm[3] = in.readInt();

        }
    }

    public void analyze() {
        System.out.println(stat.size());
        List<Map.Entry<String, PizdStat>> l = new ArrayList<>(stat.entrySet());
        l.sort(Comparator.comparingDouble(c -> c.getValue().goodness()));
        System.out.println("worst: ");
        for (int i = 0;i<50;i++) {
            PizdStat p = l.get(i).getValue();
            System.out.println(l.get(i).getKey()+" "+ p.effect()+"("+Arrays.toString(p.cm)+")");
        }
        System.out.println("");
        System.out.println("best: ");
        for (int i = l.size()-1;i>=l.size()-50;i--) {
            PizdStat p = l.get(i).getValue();
            System.out.println(l.get(i).getKey()+" "+ p.effect()+"("+Arrays.toString(p.cm)+")");
        }
        System.out.println("");
        Collections.shuffle(l);
        System.out.println("all: ");
        for (int i = 0;i<250;i++) {
            PizdStat p = l.get(i).getValue();
            System.out.println(l.get(i).getKey()+" "+ p.effect()+"("+Arrays.toString(p.cm)+")");
        }
    }

    public ArrayList<String> makeAttributes(){
        HashSet<String> set = new HashSet<>();
        for (String key : stat.keySet()){
            String[] ss = key.split(",");
            for (String s : ss)
                set.add(s);
        }
        ArrayList<String> res = new ArrayList<>(set);
        res.sort(Comparator.naturalOrder());
        return res;
    }

    public Instances makeSet(){
        ArrayList<String> ss = makeAttributes();
        ArrayList<Attribute> attributes = new ArrayList<>();
        HashMap<String,Integer> attIndexes = new HashMap<>();
        int n = 0;
        for (String s : ss) {
            attributes.add(new Attribute(s, Arrays.asList("false", "true")));
            attIndexes.put(s,n);
            n++;
        }
        attributes.add(new Attribute("effect", Arrays.asList("false", "true")));

        Instances set = new Instances("pizd", attributes, ss.size());
        for (String key : stat.keySet()){
            String[] ats = key.split(",");
            double[] d = new double[attributes.size()];
            Arrays.fill(d,0);
            PizdStat p = stat.get(key);
            d[d.length-1] = p.effect()>0.5?1:0;
            for (String a : ats)
                d[attIndexes.get(a)] = 1;
            set.add(new DenseInstance(1,d));
        }
        set.setClassIndex(attributes.size()-1);
        return set;
    }

    class PizdStat {
        int count;
        int good;
        int[] cm = new int[4];

        public double effect() {
            return ((double)cm[0]+cm[3])/count;
        }

        public double goodness() {
            return (kappa()-0.5)*Math.sqrt(count);
        }

        private double kappa(){
            double[] sumRows = new double[2];
            double[] sumColumns = new double[2];
            double sumOfWeights = 0.0D;

            for(int i = 0; i < 2; ++i) {
                for(int j = 0; j < 2; ++j) {
                    sumRows[i] += cm[i*2 + j];
                    sumColumns[j] += cm[i*2+j];
                    sumOfWeights += cm[i*2+j];
                }
            }

            double correct = 0.0D;
            double chanceAgreement = 0.0D;

            for(int i = 0; i < 2; ++i) {
                chanceAgreement += sumRows[i] * sumColumns[i];
                correct += cm[i*2+i];
            }

            chanceAgreement /= sumOfWeights * sumOfWeights;
            correct /= sumOfWeights;
            if (chanceAgreement < 1.0D) {
                return (correct - chanceAgreement) / (1.0D - chanceAgreement);
            } else {
                return 1.0D;
            }

        }

    }
}

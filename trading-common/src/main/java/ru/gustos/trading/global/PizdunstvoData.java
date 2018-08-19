package ru.gustos.trading.global;

import kotlin.Pair;
import ru.gustos.trading.book.indicators.VecUtils;
import weka.core.Instances;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class PizdunstvoData {
    public static PizdunstvoData pdbuy = new PizdunstvoData();
    public static PizdunstvoData pdsell = new PizdunstvoData();
    Hashtable<String, Hashtable<Integer, ListPizdunov>> map = new Hashtable<>();
    ArrayList<String> attributes;

    public void add(Instances set, String instrument, int month, double[][] pizd, double[][] pizd2) {
        if (attributes == null) {
            attributes = new ArrayList<>();
            for (int i = 0; i < set.numAttributes(); i++)
                attributes.add(set.attribute(i).name());
        }
        Hashtable<Integer, ListPizdunov> hh = map.computeIfAbsent(instrument, k -> new Hashtable<>());

        ListPizdunov l = hh.get(month);
        if (l == null) {
            l = new ListPizdunov();
            hh.put(month, l);
        }

        l.add(pizd, pizd2);
    }

    public void analyze(){
        if (attributes==null) return;
        int[] inWorst = new int[attributes.size()];
        int[] inBest = new int[attributes.size()];
        int[] inMid = new int[attributes.size()];
        for (String inst : map.keySet()){
            Hashtable<Integer, ListPizdunov> h = map.get(inst);
            for (int month : h.keySet()){
                ListPizdunov list = h.get(month);
                list.analyze(inWorst,inBest, inMid);
            }
        }
        ArrayList<Pair<Integer,Integer>> worst = new ArrayList<>();
        ArrayList<Pair<Integer,Integer>> best = new ArrayList<>();
        ArrayList<Pair<Integer,Integer>> mid = new ArrayList<>();
        for (int i = 0;i<attributes.size();i++){
            worst.add(new Pair<>(i,inWorst[i]));
            best.add(new Pair<>(i,inBest[i]));
            mid.add(new Pair<>(i,inMid[i]));
        }
        worst.sort(Comparator.comparing(Pair::getSecond));
        best.sort(Comparator.comparing(Pair::getSecond));
        mid.sort(Comparator.comparing(Pair::getSecond));
        System.out.print("worst: ");
        for (int i = 0;i<attributes.size();i++) {
            Pair<Integer, Integer> w = worst.get(attributes.size() - 1 - i);
            int n = w.getFirst();
            System.out.print(String.format("%s=%d(%d,%d)", attributes.get(n),w.getSecond(),inMid[n],inBest[n]));
        }
        System.out.println();
        System.out.print("best: ");
        for (int i = 0;i<attributes.size();i++) {
            Pair<Integer, Integer> w = best.get(attributes.size() - 1 - i);
            int n = w.getFirst();
            System.out.print(String.format("%s=%d(%d,%d)", attributes.get(n),w.getSecond(),inWorst[n],inMid[n]));
        }
        System.out.println();
        System.out.print("mid: ");
        for (int i = 0;i<attributes.size();i++) {
            Pair<Integer, Integer> w = mid.get(attributes.size() - 1 - i);
            int n = w.getFirst();
            System.out.print(String.format("%s=%d(%d,%d)", attributes.get(n),w.getSecond(),inWorst[n],inBest[n]));
        }
        System.out.println();

    }

    public void save(DataOutputStream out) throws IOException {
        if (attributes==null) {
            out.writeInt(0);
            return;
        }
        out.writeInt(attributes.size());
        for (int i = 0; i < attributes.size(); i++)
            out.writeUTF(attributes.get(i));
        out.writeInt(map.size());
        for (String inst : map.keySet()) {
            out.writeUTF(inst);
            Hashtable<Integer, ListPizdunov> h = map.get(inst);
            out.writeInt(h.size());
            for (int month : h.keySet()) {
                out.writeInt(month);
                ListPizdunov l = h.get(month);
                int ll = l.pizd[0].length;
                out.writeInt(ll);
                for (int i = 0; i < ll; i++) {
                    out.writeDouble(l.pizd[0][i]);
                    out.writeDouble(l.pizd[1][i]);
                    out.writeDouble(l.pizd2[0][i]);
                    out.writeDouble(l.pizd2[1][i]);
                }
            }
        }
    }

    public void load(DataInputStream in) throws IOException {
        int n = in.readInt();
        if (n==0) return;
        attributes = new ArrayList<>(n);
        while (n-- > 0)
            attributes.add(in.readUTF());
        n = in.readInt();
        while (n-- > 0) {
            String instr = in.readUTF();
            Hashtable<Integer, ListPizdunov> h = new Hashtable<>();
            map.put(instr, h);
            int n1 = in.readInt();
            while (n1-- > 0) {
                int month = in.readInt();
                int n2 = in.readInt();
                ListPizdunov l = new ListPizdunov(n2);
                h.put(month, l);
                for (int i = 0; i < n2; i++) {
                    l.pizd[0][i] = in.readDouble();
                    l.pizd[1][i] = in.readDouble();
                    l.pizd2[0][i] = in.readDouble();
                    l.pizd2[1][i] = in.readDouble();
                }
            }
        }
    }

    class ListPizdunov {
        double[][] pizd;
        double[][] pizd2;

        public ListPizdunov() {
        }

        public ListPizdunov(int size) {
            pizd = new double[2][size];
            pizd2 = new double[2][size];
        }


        public void add(double[][] p, double[][] p2) {
            if (pizd == null) {
                pizd = new double[2][p[0].length];
                pizd2 = new double[2][p2[0].length];
            }
            VecUtils.addInPlace(pizd[0], p[0]);
            VecUtils.addInPlace(pizd[1], p[1]);
            VecUtils.addInPlace(pizd2[0], p2[0]);
            VecUtils.addInPlace(pizd2[1], p2[1]);
        }

        private ArrayList<Pair<Integer, Double>> toList(double[][] pizd){
            ArrayList<Pair<Integer,Double>> res = new ArrayList<>();
            for (int i = 0;i<pizd[0].length;i++)
                res.add(new Pair<>(i,pizd[1][i]==0?0:pizd[0][i]/pizd[1][i]));
            res.sort(Comparator.comparing(Pair::getSecond));
            return res;
        }

        public void analyze(int[] inWorst, int[] inBest, int[] inMid) {
            ArrayList<Pair<Integer, Double>> p = toList(pizd);
            for (int i = 0;i<p.size()/3;i++){
                inWorst[p.get(p.size()-1-i).getFirst()]++;
                inBest[p.get(i).getFirst()]++;
                inMid[p.get(i+p.size()/3).getFirst()]++;
            }
        }
    }
}



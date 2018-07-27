package ru.gustos.trading.global;

import ru.gustos.trading.book.indicators.VecUtils;
import weka.core.Instances;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

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

    public void save(DataOutputStream out) throws IOException {
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


    }
}

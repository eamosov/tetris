package ru.gustos.trading.global;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.jetbrains.annotations.NotNull;
import ru.gustos.trading.visual.Visualizator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

public class BoundlinesFinder {
    Visualizator vis;
    public ArrayList<Line> lines;

    public BoundlinesFinder(Visualizator vis, int index) {
        this.vis = vis;
        doIt(index);
    }

    public void doIt(int index) {
        InstrumentData current = vis.current();
        current.initMinMax();

        ArrayList<MPoint> points = preparePoints(index, current);
        System.out.println("points: " + points.size());
        makeLines(points);


    }

    private void makeLines(ArrayList<MPoint> points) {
        lines = new ArrayList<>();
        do {
            double bestp = 0;
            Line best = null;
            HashSet<Integer> used = new HashSet<>();
            for (int i = 0; i < points.size() - 1; i++)
                for (int j = i + 1; j < points.size(); j++) {
                    used.clear();
                    double l = estimateLine(points, points.get(i), points.get(j), used);
                    if (l > bestp) {
                        best = new Line(points.get(i), points.get(j), l);
                        best.usedPoints = used;
                        used = new HashSet<>();
                        bestp = l;
                    }
                }
            lines.add(best);
        } while (lines.size() < 5);
    }

    private double estimateLine(ArrayList<MPoint> points, MPoint p1, MPoint p2, HashSet<Integer> used) {
        double res = 0;
        double dy = p2.y - p1.y;
        double dx = p2.x - p1.x;
        double dd = Math.sqrt(dx * dx + dy * dy);
        double a = p2.x * p1.y - p2.y * p1.x;
        lines.forEach(l->l.tempUsed = -1);

        for (int i = 0; i < points.size(); i++) {
            MPoint p = points.get(i);
            double rr = Math.abs(dy * p.x - dx * p.y + a);
            rr = rr / dd;
            double l = vis.current().avgStep * Math.sqrt(p.pow);
            if (rr < l) {
//                System.out.println("rr= "+rr+ String.format(", step*pow=%.3g*%.3g", vis.current().avgStep,p.pow));
                for (Line ll : lines){
                    if (ll.usedPoints.contains(p.x)){
                        if (ll.tempUsed<0)
                            ll.tempUsed = p.x;
                        else if (Math.abs(p.x-ll.tempUsed)>30)
                            return 0;
                    }
                }
                res += p.pow / (1 + Math.exp(rr  / l));
                used.add(p.x);
            }
        }
        return res;
    }

    private ArrayList<MPoint> preparePoints(int index, InstrumentData current) {
        ArrayList<MPoint> points = new ArrayList<>();
        for (int i = index; i >= 0; i--) {
            int d = 31 - Integer.numberOfLeadingZeros(index - i);
            d = Math.max(0, d - 3);
            if (current.minPow[i] > d) {
                points.add(new MPoint(i, current.bar(i).getMinPrice(), current.minPow[i]+1));
            } else if (current.maxPow[i] > d) {
                points.add(new MPoint(i, current.bar(i).getMaxPrice(), current.maxPow[i]+1));
            }
        }
        points.sort(Comparator.comparingInt(p -> p.x));
        return points;
    }

    public ArrayList<SimpleRegression> regressions() {
        ArrayList<SimpleRegression> res = new ArrayList<>();
        for (Line l : lines) {
            res.add(l.regression());
        }
        return res;
    }

    class MPoint {
        int x;
        double y;
        double price;
        double pow;

        public MPoint(int x, float y, double pow) {
            this.x = x;
            this.y = y;
            this.price = y;
            this.pow = pow;
        }
    }

    class Line implements Comparable {
        MPoint p1;
        MPoint p2;
        double pow;
        HashSet<Integer> usedPoints;
        int tempUsed;

        public Line(MPoint p1, MPoint p2, double p) {
            this.p1 = p1;
            this.p2 = p2;
            this.pow = p;
        }

        @Override
        public int compareTo(@NotNull Object o) {
            if (o instanceof Line)
                return Double.compare(pow, ((Line) o).pow);

            return 0;
        }

        public SimpleRegression regression() {
            SimpleRegression reg = new SimpleRegression();
            reg.addData(p1.x, p1.price);
            reg.addData(p2.x, p2.price);
            return reg;
        }

        @Override
        public String toString() {
            return String.format("p1 pow %.3g, p2 pow %.3g, pow %.3g", p1.pow, p2.pow, pow);
        }
    }
}

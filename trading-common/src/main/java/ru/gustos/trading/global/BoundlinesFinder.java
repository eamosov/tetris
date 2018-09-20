package ru.gustos.trading.global;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.jetbrains.annotations.NotNull;
import ru.gustos.trading.visual.Visualizator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

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
        normPoints(points);


    }

    private void normPoints(ArrayList<MPoint> points) {
//        double min = points.stream().mapToDouble(p -> p.y).min().getAsDouble();
//        double max = points.stream().mapToDouble(p -> p.y).max().getAsDouble();
//
//        int minx = points.stream().mapToInt(p -> p.x).min().getAsInt();
//        int maxx = points.stream().mapToInt(p -> p.x).max().getAsInt();

//        double k = (maxx - minx) / (max - min);
        PriorityQueue<Line> q = new PriorityQueue<>();
//        points.forEach(p -> p.y *= k);
        for (int i = 0; i < points.size() - 1; i++)
            for (int j = i + 1; j < points.size(); j++) {
                double l = estimateLine(points, points.get(i), points.get(j));
                q.add(new Line(points.get(i), points.get(j), l));
                if (q.size() > 5) {
                    Line poll = q.poll();
//                    System.out.println(poll.pow);
                }
            }
        lines = new ArrayList<>(q);
            System.out.println(lines);
    }

    private double estimateLine(ArrayList<MPoint> points, MPoint p1, MPoint p2) {
        double res = 0;
        double dy = p2.y - p1.y;
        double dx = p2.x - p1.x;
        double dd = Math.sqrt(dx * dx + dy * dy);
        double a = p2.x * p1.y - p2.y * p1.x;

        for (int i = 0; i < points.size(); i++) {
            MPoint p = points.get(i);
            double rr = Math.abs(dy * p.x - dx * p.y + a);
            rr = rr / dd;
            double l = vis.current().avgStep * Math.sqrt(p.pow);
            if (rr< l) {
//                System.out.println("rr= "+rr+ String.format(", step*pow=%.3g*%.3g", vis.current().avgStep,p.pow));
                res += p.pow/(1+Math.exp(rr*5/l));
            }
        }
        return res;
    }

    private ArrayList<MPoint> preparePoints(int index, InstrumentData current) {
        ArrayList<MPoint> points = new ArrayList<>();
        for (int i = index; i >= 0; i--) {
            int d = 31 - Integer.numberOfLeadingZeros(index - i);
            d = Math.max(2,d-3);
            if (current.minPow[i] > d) {
                points.add(new MPoint(i, current.bar(i).getMinPrice(), current.minPow[i]));
            } else if (current.maxPow[i] > d) {
                points.add(new MPoint(i, current.bar(i).getMaxPrice(), current.maxPow[i]));
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
            return String.format("p1 pow %.3g, p2 pow %.3g, pow %.3g", p1.pow,p2.pow,pow);
        }
    }
}

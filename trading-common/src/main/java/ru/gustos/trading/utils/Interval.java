package ru.gustos.trading.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Interval {
    public int start;
    public int end;

    public Interval() {
        start = 0;
        end = 0;
    }

    public Interval(int s, int e) {
        start = s;
        end = e;
    }

    public static int sumSize(ArrayList<Interval> intervals){
        int r = 0;
        for (int i = 0;i<intervals.size();i++)
            r+=intervals.get(i).length();
        return r;

    }

    private int length() {
        return end-start;
    }

    public static ArrayList<Interval> merge(ArrayList<Interval> intervals) {
        // Start typing your Java solution below
        // DO NOT write main() function
        if (intervals.size() == 0)
            return intervals;
        if (intervals.size() == 1)
            return intervals;

        Collections.sort(intervals, new IntervalComparator());

        Interval first = intervals.get(0);
        int start = first.start;
        int end = first.end;

        ArrayList<Interval> result = new ArrayList<Interval>();

        for (int i = 1; i < intervals.size(); i++) {
            Interval current = intervals.get(i);
            if (current.start <= end) {
                end = Math.max(current.end, end);
            } else {
                result.add(new Interval(start, end));
                start = current.start;
                end = current.end;
            }

        }

        result.add(new Interval(start, end));

        return result;

    }

    static class IntervalComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            Interval i1 = (Interval) o1;
            Interval i2 = (Interval) o2;
            return i1.start - i2.start;
        }
    }
}


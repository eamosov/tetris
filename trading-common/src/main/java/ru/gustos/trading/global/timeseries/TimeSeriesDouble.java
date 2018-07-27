package ru.gustos.trading.global.timeseries;

import java.util.Arrays;
import java.util.stream.Collectors;

public class TimeSeriesDouble{
    double[] data;
    long[] times;
    int size;

    public TimeSeriesDouble(){
        this(10);
    }

    public TimeSeriesDouble(int capacity){
        data = new double[capacity];
        times = new long[capacity];
    }

    public static TimeSeriesDouble fromString(String s){
        int p = s.indexOf('\n');
        String[] times = s.substring(0, p).trim().split(",");
        String[] data = s.substring(p+1).trim().split(",");
        TimeSeriesDouble result = new TimeSeriesDouble(times.length);
        for (int i = 0;i<times.length;i++)
            result.add(Double.parseDouble(data[i]),Long.parseLong(times[i]));
        return result;
    }

    public void add(double value, long time){
        int i;
        if (times.length <= size) {
            times = Arrays.copyOf(times, times.length * 2);
            data = Arrays.copyOf(data, data.length * 2);
        }
        if (size==0 || time>times[size-1]){
            i = size;
        } else {
            i = Arrays.binarySearch(times, 0,size,time);
//            if (i > 0) throw new NullPointerException("dublicate time");
            if (i < 0) i = -i - 1;
            System.arraycopy(times, i, times, i + 1, size - i);
            System.arraycopy(data, i, data, i + 1, size - i);
        }
        times[i] = time;
        data[i] = value;
        size++;
    }

    public int findIndex(long time){
        int i = Arrays.binarySearch(times, 0,size,time);
        if (i==-1) return -1;
        if (i<0) {
            i = -i-1;
            if (i>0) i--;
        }
        return i;

    }

    public double getAt(long time){
        int index = findIndex(time);
        if (index<0) return Double.NaN;
        return data[index];
    }

    public int size(){
        return size;
    }

    public double get(int index){
        return data[index];
    }

    public long time(int index){
        return times[index];
    }

    public long getBeginTime() {
        return times[0];
    }

    public long getEndTime() {
        return times[size-1];
    }

    public String toString(){
        return Arrays.stream(times).limit(size).mapToObj(Long::toString).collect(Collectors.joining(",")) + "\n" +
                Arrays.stream(data).limit(size).mapToObj(Double::toString).collect(Collectors.joining(","));
    }

}


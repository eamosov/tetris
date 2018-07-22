package ru.gustos.trading.global;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;

public class TimeSeries<T>{
    ArrayList<T> data;
    long[] times;

    public TimeSeries(){
        this(10);
    }

    public TimeSeries(int capacity){
         data = new ArrayList<>(capacity);
         times = new long[capacity];
    }

    public void add(T element, long time){
        int i;
        if (times.length <= data.size())
            times = Arrays.copyOf(times, times.length * 2);
        if (data.size()==0 || time>times[data.size()-1]){
            i = data.size();
        } else {
            i = Arrays.binarySearch(times, time);
            if (i > 0) throw new NullPointerException("dublicate time");
            if (i < 0) i = -i - 1;
            System.arraycopy(times, i, times, i + 1, data.size() - i);
        }
        times[i] = time;
        data.add(i,element);
    }

    public int findIndex(long time){
        int i = Arrays.binarySearch(times, time);
        if (i==-1) return -1;
        if (i<0) {
            i = -i-1;
            if (i>0) i--;
        }
        return i;

    }

    public T getAt(long time){
        int index = findIndex(time);
        if (index<0) return null;
        return data.get(index);
    }

    public int size(){
        return data.size();
    }

    public T get(int index){
        return data.get(index);
    }

    public long getBeginTime() {
        return times[0];
    }

    public long getEndTime() {
        return times[data.size()-1];
    }
}

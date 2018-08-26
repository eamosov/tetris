package ru.efreet.trading.utils;

import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.bars.XBaseBar;
import ru.gustos.trading.book.indicators.EmaRecurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BarsPacker {

    public static List<? extends XBar> packBarsVolumeAvg(List<? extends XBar> list, int minutes){
        double[] dd = list.stream().mapToDouble(XBar::getVolume).toArray();
        Arrays.sort(dd);
        double avg = dd[dd.length/2];
//        System.out.println(avg);
//        avg = 1.6;
        return packBarsVolume(list,minutes*avg);
    }

    public static List<? extends XBar> packBarsVolume(List<? extends XBar> list, double volume){
        ArrayList<XBaseBar> result = new ArrayList<XBaseBar>();
        XBaseBar last = null;
        for (XBar b : list){
            if (last==null || last.getVolume()>=volume){
                last = new XBaseBar(b);
                result.add(last);
            } else {
                last.addBar(b);
            }
        }
        return result;

    }

    public static ArrayList<XBaseBar> packBarsVolumeEma(List<? extends XBar> list, int window, double k){
        ArrayList<XBaseBar> result = new ArrayList<XBaseBar>();
        XBaseBar last = null;
        EmaRecurrent ema = new EmaRecurrent(window);
        for (XBar b : list){
            double avg = ema.feed(b.getVolume());
            if (last==null || last.getVolume()>=avg*k){
                last = new XBaseBar(b);
                result.add(last);
            } else {
                last.addBar(b);
            }
        }
        return result;

    }

    public static ArrayList<XBaseBar> packBars(List<? extends XBar> list, int cnt){
        ArrayList<XBaseBar> result = new ArrayList<XBaseBar>();
        XBaseBar last = null;
        int t = 0;
        for (XBar b : list){
            t++;
            if (last==null || t>cnt){
                last = new XBaseBar(b);
                result.add(last);
                t = 0;
            } else {
                last.addBar(b);
            }
        }
        return result;

    }

    public static ArrayList<XBaseBar> packBarsSign(List<? extends XBar> list){
        ArrayList<XBaseBar> result = new ArrayList<XBaseBar>();
        XBaseBar last = null;
        for (XBar b : list){
            if (last==null || last.isBullish()!=b.isBullish()){
                last = new XBaseBar(b);
                result.add(last);
            } else {
                last.addBar(b);
            }
        }
        return result;

    }

    public static List<XBaseBar> invertBars(List<XBaseBar> bars) {
        ArrayList<XBaseBar> result = new ArrayList<XBaseBar>();
        for (XBar b : bars) {
            XBaseBar bb = new XBaseBar(b);
            bb.invert();
            result.add(bb);
        }

        return result;
    }
}

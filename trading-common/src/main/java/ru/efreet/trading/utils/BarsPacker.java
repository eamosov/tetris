package ru.efreet.trading.utils;

import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.bars.XBaseBar;

import java.util.ArrayList;
import java.util.List;

public class BarsPacker {
    public static ArrayList<XBaseBar> packBarsVolume(List<? extends XBar> list, int volume){
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

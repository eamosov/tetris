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

}

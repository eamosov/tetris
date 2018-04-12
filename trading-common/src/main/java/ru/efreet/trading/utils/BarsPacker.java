package ru.efreet.trading.utils;

import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.bars.XBaseBar;

import java.util.ArrayList;
import java.util.List;

public class BarsPacker {
    public static ArrayList<XBaseBar> packBars(List<XBar> list, int volume){
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
}

package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.bars.XBaseBar;

public class VolumeBarRecurrent {
    XBaseBar bar = null;
    EmaRecurrent volumeLong;
    int bars;

    public VolumeBarRecurrent(int bars) {
        volumeLong = new EmaRecurrent(1000);
        this.bars = bars;
    }

    public boolean feed(XBar b) {
        volumeLong.feed(b.getVolume());
        if (bar==null || bar.getVolume()>volumeLong.value()*bars)
            bar = new XBaseBar(b);
        else
            bar.addBar(b);
        return bar.getVolume()>volumeLong.value()*bars;
    }

    public XBar bar(){
        return bar;
    }

}



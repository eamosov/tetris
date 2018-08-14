package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.bars.XBaseBar;

public class VolumeBarRecurrent {
    XBaseBar bar = null;
    EmaRecurrent volumeLong;

    public VolumeBarRecurrent() {
        volumeLong = new EmaRecurrent(1000);
    }

    public boolean feed(XBar b) {
        volumeLong.feed(b.getVolume());
        if (bar==null || bar.getVolume()>volumeLong.value()*10)
            bar = new XBaseBar(b);
        else
            bar.addBar(b);
        return bar.getVolume()>volumeLong.value()*10;
    }

    public XBar bar(){
        return bar;
    }

}

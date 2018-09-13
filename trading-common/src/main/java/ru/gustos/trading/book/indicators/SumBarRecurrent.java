package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBar;
import ru.efreet.trading.bars.XBaseBar;

public class SumBarRecurrent {
    XBaseBar bar = null;
    int cnt;
    int cc = 0;

    public SumBarRecurrent(int cnt) {
        this.cnt = cnt;
    }

    public boolean feed(XBar b) {
        if (bar==null || cc>=cnt)
            bar = new XBaseBar(b);
        else
            bar.addBar(b);
        cc++;
        return cc>=cnt;
    }

    public XBar bar(){
        return bar;
    }

}

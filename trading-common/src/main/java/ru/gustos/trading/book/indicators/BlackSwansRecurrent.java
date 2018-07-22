package ru.gustos.trading.book.indicators;

import ru.efreet.trading.bars.XBaseBar;

public class BlackSwansRecurrent {
    EmaRecurrent ema;
    EmaRecurrent sd;
    int fromLastSwan;

    public BlackSwansRecurrent(){
        ema = new EmaRecurrent(30);
        sd = new EmaRecurrent(30);
    }

    public void feed(XBaseBar bar){

    }

}


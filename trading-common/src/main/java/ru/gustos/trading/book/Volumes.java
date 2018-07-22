package ru.gustos.trading.book;

import ru.efreet.trading.bars.XBar;

public class Volumes extends PriceHistogram {


    public Volumes(BarsSource sheet, boolean calc, boolean needByDays){
        super(sheet, needByDays);
        if (calc)
            calc(sheet.size() - 1);
    }

    @Override
    protected void process(int index, double[] v1, double[] v2) {
        XBar bar = sheet.bar(index);
        if (bar.isBearish())
            add(v1,bar, bar.getVolume());
        else
            add(v2,bar, bar.getVolume());

    }


}



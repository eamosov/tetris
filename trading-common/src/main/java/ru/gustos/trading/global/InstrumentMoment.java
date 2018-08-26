package ru.gustos.trading.global;

import ru.efreet.trading.bars.XBar;

public class InstrumentMoment implements  MomentDataProvider{
    public XBar bar;
    public MomentData mldata;  // для оценки
    public MomentData mldata2;  // для выбора момента

    public InstrumentMoment(XBar bar) {
        this.bar = bar;
        mldata = new MomentData(100);
        mldata2 = new MomentData(300);
    }


    @Override
    public MomentData getMomentData() {
        return mldata;
    }
}

package ru.gustos.trading.book.indicators;

import ru.efreet.trading.Decision;
import ru.efreet.trading.bars.XBar;

public class LevelsTrader {
    int fixTime;
    double fixAmp;

    boolean high;
    int sideTime = -1;

    boolean fixedHigh = false;
    EmaRecurrent sd;
    EmaRecurrent longVolume;
    EmaRecurrent shortVolume;
    boolean wasAmp = false;
    StohasticRecurrent stoh;
    Decision stohastic = Decision.NONE;

    public LevelsTrader(int fixTime, int sellSdTimeFrame, double fixAmp){
        this.fixTime = fixTime;
        this.fixAmp = fixAmp;
        sd = new EmaRecurrent(sellSdTimeFrame);
        longVolume = new EmaRecurrent(300);
        shortVolume = new EmaRecurrent(20);
        stoh = new StohasticRecurrent(10,90,10,75,25);
    }

    public void feed(XBar bar, double level){
        if (sideTime==-1){
            sideTime = 0;
            high = bar.getClosePrice()>level;
            sd.feed(0);
            longVolume.feed(bar.getVolume());
            shortVolume.feed(bar.getVolume());
            stohastic = stoh.feed(bar);
            return;
        }
        longVolume.feed(bar.getVolume());
        shortVolume.feed(bar.getVolume());
        stohastic = stoh.feed(bar);
        double d = Math.abs(bar.getClosePrice() - level);
        sd.feed(d*d,bar.getVolume()/longVolume());
        boolean nowHigh = bar.getClosePrice()>level;
        if (nowHigh==high){
            sideTime++;
            if (Math.sqrt(sd.value())/bar.getClosePrice()>fixAmp)
                wasAmp = true;
            if (sideTime>fixTime && wasAmp)
                fixedHigh = high;
        } else {
            sideTime = 0;
            wasAmp = false;
            high = nowHigh;
        }
        if (high!=fixedHigh && (bar.getVolume()>longVolume.value()*15 || shortVolume.value()>longVolume.value()*5) && high==bar.isBullish()){
            fixedHigh = high;
        }
    }

    public boolean high(){
        return fixedHigh;
    }

    public Decision stohastic(){ return stohastic;}

    public double sd(){
        return Math.sqrt(sd.value());
    }

    public double longVolume(){
        return longVolume.value();
    }

    public double shortVolume(){
        return shortVolume.value();
    }

}

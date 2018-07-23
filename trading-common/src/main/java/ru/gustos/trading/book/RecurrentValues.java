package ru.gustos.trading.book;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.indicators.*;
import ru.gustos.trading.global.GustosLogicOptimizator;

public class RecurrentValues {
    public BarsSource sheet;
    public EmaRecurrent ema1 = new EmaRecurrent(20);
    public EmaRecurrent ema2 = new EmaRecurrent(100);
    public EmaRecurrent ema3 = new EmaRecurrent(700);
    public EmaRecurrent sd1 = new EmaRecurrent(20);
    public EmaRecurrent sd2 = new EmaRecurrent(100);
    public EmaRecurrent sd3 = new EmaRecurrent(700);
    public EmaRecurrent volumeLong = new EmaRecurrent(700);
    public EmaRecurrent volumeShort = new EmaRecurrent(5);
    public EmaRecurrent deltaToVolume = new EmaRecurrent(500);
    public EmaRecurrent deltaToVolumeShort = new EmaRecurrent(5);
    public EmaRecurrent maxminToVolume = new EmaRecurrent(500);
    public EmaRecurrent maxminToVolumeShort = new EmaRecurrent(5);
    public EmaRecurrent maxmin = new EmaRecurrent(500);
    public EmaRecurrent maxminShort = new EmaRecurrent(5);
    public EmaRecurrent change1 = new EmaRecurrent(5);
    public EmaRecurrent change2 = new EmaRecurrent(50);
    public EmaRecurrent change3 = new EmaRecurrent(500);
    public GustosAverageRecurrent gustosAvg = new GustosAverageRecurrent(170, 1700, 30);
    public GustosAverageRecurrent gustosAvg2 = new GustosAverageRecurrent(17, 170, 3);
    public GustosAverageRecurrent gustosAvgBuy = new GustosAverageRecurrent(177, 1842, 30);
    public GustosAverageRecurrent gustosAvgSell = new GustosAverageRecurrent(702, 1891, 30);
    public StohasticRecurrent stoh1 = new StohasticRecurrent(30, 80, 20, 90, 10);
    public StohasticRecurrent stoh2 = new StohasticRecurrent(300, 80, 20, 90, 10);
    public MacdRecurrent macd1 = new MacdRecurrent(25, 60, 17);
    public MacdRecurrent macd2 = new MacdRecurrent(250, 600, 170);
    public RsiRecurrent rsi1 = new RsiRecurrent(100);
    public RsiRecurrent rsi2 = new RsiRecurrent(1000);
    public RsiWithVolumesRecurrent rsiv1 = new RsiWithVolumesRecurrent(100);
    public RsiWithVolumesRecurrent rsiv2 = new RsiWithVolumesRecurrent(1000);

    public GustosLogicOptimizator.Params gustosParams = new GustosLogicOptimizator.Params();

    public RecurrentValues(BarsSource sheet) {
        this.sheet = sheet;
    }

    public void feed(int index) {
        XBar bar = sheet.bar(index);
        double p = bar.getClosePrice();
        double volume = bar.getVolume();
        ema1.feed(p);
        ema2.feed(p);
        ema3.feed(p);
        sd1.feed((p-ema1.value())*(p-ema1.value()));
        sd2.feed((p-ema2.value())*(p-ema2.value()));
        sd3.feed((p-ema3.value())*(p-ema3.value()));
        gustosAvg.feed(p, volume);
        gustosAvg2.feed(p, volume);
        gustosAvgBuy.feed(p, volume);
        gustosAvgSell.feed(p, volume);
        stoh1.feed(bar);
        stoh2.feed(bar);
        macd1.feed(p);
        macd2.feed(p);
        volumeLong.feed(volume);
        volumeShort.feed(volume);
        rsi1.feed(p);
        rsi2.feed(p);
        rsiv1.feed(p, volume);
        rsiv2.feed(p, volume);
        double d2v = volume /Math.max(bar.middlePrice()/100000.0,bar.delta());
        deltaToVolume.feed(d2v);
        deltaToVolumeShort.feed(d2v);
        d2v = volume /Math.max(bar.middlePrice()/100000.0,bar.deltaMaxMin());
        maxminToVolume.feed(d2v);
        maxminToVolumeShort.feed(d2v);

        maxmin.feed(bar.deltaMaxMin());
        maxminShort.feed(bar.deltaMaxMin());

        change1.feed((bar.getClosePrice()/sheet.bar(Math.max(0,index-10)).getClosePrice()-1)*100);
        change2.feed((bar.getClosePrice()/sheet.bar(Math.max(0,index-100)).getClosePrice()-1)*100);
        change3.feed((bar.getClosePrice()/sheet.bar(Math.max(0,index-1000)).getClosePrice()-1)*100);
    }



    public void setGustosParams(GustosLogicOptimizator.Params params) {
        gustosParams = params;
        gustosAvgBuy.changeParams(params.buyWindow(),params.buyVolumeWindow(),params.volumeShort(),params.volumePow1(),params.volumePow2());
        gustosAvgSell.changeParams(params.sellWindow(),params.sellVolumeWindow(),params.volumeShort(),params.volumePow1(),params.volumePow2());
    }
}

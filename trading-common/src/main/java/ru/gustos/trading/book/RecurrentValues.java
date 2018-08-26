package ru.gustos.trading.book;

import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.indicators.*;
import ru.gustos.trading.global.GustosLogicOptimizator;

public class RecurrentValues {
    public BarsSource sheet;
    public EmaRecurrent ema0 = new EmaRecurrent(5);
    public EmaRecurrent ema1 = new EmaRecurrent(20);
    public EmaRecurrent ema2 = new EmaRecurrent(100);
    public EmaRecurrent ema3 = new EmaRecurrent(700);
    public EmaRecurrent sd0 = new EmaRecurrent(5);
    public EmaRecurrent sd1 = new EmaRecurrent(20);
    public EmaRecurrent sd2 = new EmaRecurrent(100);
    public EmaRecurrent sd3 = new EmaRecurrent(700);
    public EmaRecurrent volumeLong = new EmaRecurrent(700);
    public EmaRecurrent volumeShort = new EmaRecurrent(3);
    public EmaRecurrent volumeLong2 = new EmaRecurrent(1700);
    public EmaRecurrent volumeShort2 = new EmaRecurrent(10);
    public EmaRecurrent deltaToVolume = new EmaRecurrent(500);
    public EmaRecurrent deltaToVolumeShort = new EmaRecurrent(5);
    public EmaRecurrent deltaToMm = new EmaRecurrent(500);
    public EmaRecurrent deltaToMmShort = new EmaRecurrent(5);
    public EmaRecurrent maxminToVolume = new EmaRecurrent(500);
    public EmaRecurrent maxminToVolumeShort = new EmaRecurrent(5);
    public EmaRecurrent maxmin = new EmaRecurrent(500);
    public EmaRecurrent maxminShort = new EmaRecurrent(5);
    public EmaRecurrent change0 = new EmaRecurrent(3);
    public EmaRecurrent change1 = new EmaRecurrent(5);
    public EmaRecurrent change2 = new EmaRecurrent(50);
    public EmaRecurrent change3 = new EmaRecurrent(500);
    public EmaRecurrent change4 = new EmaRecurrent(1000);
    public GustosAverageRecurrent gustosAvg = new GustosAverageRecurrent(170, 1700, 30);
    public GustosAverageRecurrent gustosAvg2 = new GustosAverageRecurrent(17, 170, 3);
    public GustosAverageRecurrent gustosAvg3 = new GustosAverageRecurrent(85, 850, 20);
    public GustosAverageRecurrent gustosAvg4 = new GustosAverageRecurrent(700, 1900, 30);
//    public GustosAverageRecurrent gustosAvgBuy = new GustosAverageRecurrent(177, 1842, 30);
//    public GustosAverageRecurrent gustosAvgSell = new GustosAverageRecurrent(702, 1891, 30);
    public StohasticRecurrent stoh0 = new StohasticRecurrent(5, 80, 20, 90, 10);
    public StohasticRecurrent stoh1 = new StohasticRecurrent(30, 80, 20, 90, 10);
    public StohasticRecurrent stoh2 = new StohasticRecurrent(300, 80, 20, 90, 10);
    public MacdRecurrent macd0 = new MacdRecurrent(3, 7, 5);
    public MacdRecurrent macd1 = new MacdRecurrent(25, 60, 17);
    public MacdRecurrent macd2 = new MacdRecurrent(250, 600, 170);
    public MacdRecurrent macd3 = new MacdRecurrent(70, 200, 50);
    public MacdRecurrent macd4 = new MacdRecurrent(700, 2000, 500);
    public DemaRecurrent dema1 = new DemaRecurrent(25, 60, 17);
    public DemaRecurrent dema2 = new DemaRecurrent(250, 600, 170);
    public DemaRecurrent dema3 = new DemaRecurrent(70, 200, 50);
    public DemaRecurrent vdema0 = new DemaRecurrent(3, 7, 5);
    public DemaRecurrent vdema1 = new DemaRecurrent(25, 60, 17);
    public DemaRecurrent vdema2 = new DemaRecurrent(250, 600, 170);
    public DemaRecurrent vdema3 = new DemaRecurrent(70, 200, 50);
    public DemaRecurrent vdema4 = new DemaRecurrent(700, 2000, 500);
    public RsiRecurrent rsi0 = new RsiRecurrent(10);
    public RsiRecurrent rsi1 = new RsiRecurrent(100);
    public RsiRecurrent rsi2 = new RsiRecurrent(1000);
    public RsiWithVolumesRecurrent rsiv0 = new RsiWithVolumesRecurrent(10);
    public RsiWithVolumesRecurrent rsiv1 = new RsiWithVolumesRecurrent(100);
    public RsiWithVolumesRecurrent rsiv2 = new RsiWithVolumesRecurrent(1000);

    public VolumeBarRecurrent volumeBar = new VolumeBarRecurrent();
    public MacdRecurrent vmacd0 = new MacdRecurrent(3, 7, 5);
    public MacdRecurrent vmacd1 = new MacdRecurrent(25, 60, 17);
    public MacdRecurrent vmacd2 = new MacdRecurrent(70, 200, 50);


    public GustosLogicOptimizator.Params gustosParams = new GustosLogicOptimizator.Params();

    public RecurrentValues(BarsSource sheet) {
        this.sheet = sheet;
    }

    public void feed(int index) {
        XBar bar = sheet.bar(index);
        double p = bar.getClosePrice();
        double volume = bar.getVolume();
        ema0.feed(p);
        ema1.feed(p);
        ema2.feed(p);
        ema3.feed(p);
        sd0.feed((p-ema0.value())*(p-ema0.value()));
        sd1.feed((p-ema1.value())*(p-ema1.value()));
        sd2.feed((p-ema2.value())*(p-ema2.value()));
        sd3.feed((p-ema3.value())*(p-ema3.value()));
        gustosAvg.feed(p, volume);
        gustosAvg2.feed(p, volume);
        gustosAvg3.feed(p, volume);
        gustosAvg4.feed(p, volume);
//        gustosAvgBuy.feed(p, volume);
//        gustosAvgSell.feed(p, volume);
        stoh0.feed(bar);
        stoh1.feed(bar);
        stoh2.feed(bar);
        macd0.feed(p);
        macd1.feed(p);
        macd2.feed(p);
        macd3.feed(p);
        macd4.feed(p);
        dema1.feed(p);
        dema2.feed(p);
        dema3.feed(p);
        volumeLong.feed(volume);
        volumeShort.feed(volume);
        volumeLong2.feed(volume);
        volumeShort2.feed(volume);
        rsi0.feed(p);
        rsi1.feed(p);
        rsi2.feed(p);
        rsiv0.feed(p, volume);
        rsiv1.feed(p, volume);
        rsiv2.feed(p, volume);
        double d2v = volume /Math.max(bar.middlePrice()/100000.0,bar.delta());
        deltaToVolume.feed(d2v);
        deltaToVolumeShort.feed(d2v);
        d2v = volume /Math.max(bar.middlePrice()/100000.0,bar.deltaMaxMin());
        maxminToVolume.feed(d2v);
        maxminToVolumeShort.feed(d2v);

        double d2mm = bar.delta() / Math.max(bar.middlePrice()/100000.0,bar.deltaMaxMin());
        deltaToMm.feed(d2mm);
        deltaToMmShort.feed(d2mm);

        maxmin.feed(bar.deltaMaxMin());
        maxminShort.feed(bar.deltaMaxMin());

        change0.feed((bar.getClosePrice()/sheet.bar(Math.max(0,index-2)).getClosePrice()-1)*100);
        change1.feed((bar.getClosePrice()/sheet.bar(Math.max(0,index-10)).getClosePrice()-1)*100);
        change2.feed((bar.getClosePrice()/sheet.bar(Math.max(0,index-100)).getClosePrice()-1)*100);
        change3.feed((bar.getClosePrice()/sheet.bar(Math.max(0,index-1000)).getClosePrice()-1)*100);
        change4.feed((bar.getClosePrice()/sheet.bar(Math.max(0,index-3000)).getClosePrice()-1)*100);

        if (volumeBar.feed(bar)){
            vmacd0.feed(volumeBar.bar().getClosePrice());
            vmacd1.feed(volumeBar.bar().getClosePrice());
            vmacd2.feed(volumeBar.bar().getClosePrice());
            vdema0.feed(volumeBar.bar().getClosePrice());
            vdema1.feed(volumeBar.bar().getClosePrice());
            vdema2.feed(volumeBar.bar().getClosePrice());
            vdema3.feed(volumeBar.bar().getClosePrice());
            vdema4.feed(volumeBar.bar().getClosePrice());
        }
    }



//    public void setGustosParams(GustosLogicOptimizator.Params params) {
//        gustosParams = params;
//        gustosAvgBuy.changeParams(params.buyWindow(),params.buyVolumeWindow(),params.volumeShort(),params.volumePow1(),params.volumePow2());
//        gustosAvgSell.changeParams(params.sellWindow(),params.sellVolumeWindow(),params.volumeShort(),params.volumePow1(),params.volumePow2());
//    }
}


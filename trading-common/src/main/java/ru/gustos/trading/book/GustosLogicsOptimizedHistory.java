package ru.gustos.trading.book;

import kotlin.Pair;
import ru.efreet.trading.bars.XBar;
import ru.gustos.trading.book.indicators.GustosAverageRecurrent;
import ru.gustos.trading.global.CalcUtils;
import ru.gustos.trading.global.GustosLogicOptimizator;
import ru.gustos.trading.global.InstrumentData;
import weka.core.stopwords.Null;

import java.util.ArrayList;

public class GustosLogicsOptimizedHistory{

    ArrayList<GustosLogicOptimizator.Params> params = new ArrayList<>();
    ArrayList<Pair<GustosAverageRecurrent,GustosAverageRecurrent>> gars = new ArrayList<>();
    InstrumentData data;
    int needIndex;

    public GustosLogicsOptimizedHistory(InstrumentData data){
        this.data = data;
    }

    public GustosLogicsOptimizedHistory clone(){
        GustosLogicsOptimizedHistory res = new GustosLogicsOptimizedHistory(data);
        res.needIndex = needIndex;
        for (GustosLogicOptimizator.Params p : params)
            res.params.add(new GustosLogicOptimizator.Params(p));
        for (int i = 0;i<gars.size();i++){
            Pair<GustosAverageRecurrent, GustosAverageRecurrent> pp = gars.get(i);
            res.gars.add(new Pair<>(pp.getFirst().clone(),pp.getSecond().clone()));
        }
        return res;
    }

    public void add(int index, int period){
        GustosLogicOptimizator opt = new GustosLogicOptimizator(data, index-period, index);
        Pair<GustosLogicOptimizator.Params, Double> r = opt.optimize(params.size()==0?new GustosLogicOptimizator.Params():params.get(params.size()-1));
        GustosLogicOptimizator.Params p = r.getFirst();
        params.add(p);
        GustosAverageRecurrent buy = new GustosAverageRecurrent(p.buyWindow(),p.buyVolumeWindow(),p.volumeShort(),p.volumePow1(),p.volumePow2());
        GustosAverageRecurrent sell = new GustosAverageRecurrent(p.sellWindow(),p.sellVolumeWindow(),p.volumeShort(),p.volumePow1(),p.volumePow2());
        gars.add(new Pair<>(buy,sell));
        for (int i = 0;i<index;i++){
            XBar bar = data.bar(i);
            buy.feedNoReturn(bar.getClosePrice(),bar.getVolume());
            sell.feedNoReturn(bar.getClosePrice(),bar.getVolume());
        }
        needIndex = index;
        while (params.size()>10){
            params.remove(0);
            gars.remove(0);
        }
    }

    public void feed(int index){
        if (needIndex!=index)
            throw new NullPointerException("feeding wrong index "+needIndex+" "+index);
        XBar bar = data.bar(index);
        for (int i = 0;i<gars.size();i++){
            gars.get(i).getFirst().feedNoReturn(bar.getClosePrice(),bar.getVolume());
            gars.get(i).getSecond().feedNoReturn(bar.getClosePrice(),bar.getVolume());
        }
        needIndex = index+1;
    }

    public double countBuys(int index){
        if (gars.size()==0) return 0;
        int res = 0;
        for (int i = 0;i<gars.size();i++){
            if (CalcUtils.gustosBuy(data,index,gars.get(i).getFirst(),params.get(i)))
                res++;
        }
        return res*1.0/gars.size();
    }

    public double countSells(int index){
        if (gars.size()==0) return 0;
        int res = 0;
        for (int i = 0;i<gars.size();i++){
            if (CalcUtils.gustosSell(data,index,gars.get(i).getSecond(),params.get(i)))
                res++;
        }
        return res*1.0/gars.size();
    }


    public int needIndex() {
        return needIndex;
    }
}

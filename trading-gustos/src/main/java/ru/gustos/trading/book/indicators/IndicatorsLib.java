package ru.gustos.trading.book.indicators;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Comparator;

public class IndicatorsLib {
    private IIndicator[] indicators;
    private IIndicator[] indicatorsShow;
    private IIndicator[] map;

    public IndicatorsLib() throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        IndicatorInitData[] initData = new Gson().fromJson(FileUtils.readFileToString(new File("d:/tetrislibs/indicators.json")),IndicatorInitData[].class);

        indicators = new IIndicator[initData.length];

        for (int i = 0;i<indicators.length;i++)
            indicators[i] = createIndicator(initData[i]);
        indicatorsShow = Arrays.stream(indicators).filter(IIndicator::showOnPane).toArray(IIndicator[]::new);

//        indicators = new IIndicator[]{new TargetDecisionIndicator(),new VolumeIndicator(),new TargetBuyIndicator(),new TargetSellIndicator(),
//                new EfreetIndicator(),new EfreetSuccessIndicator(),
//        new PriceChangeIndicator(IndicatorPeriod.TENMINUTES),new PriceChangeIndicator(IndicatorPeriod.HOUR),new PriceChangeIndicator(IndicatorPeriod.DAY),
//                new PriceChangeIndicator(IndicatorPeriod.WEEK),new PriceChangeIndicator(IndicatorPeriod.MONTH),
//        new RelativePriceIndicator(IndicatorPeriod.TENMINUTES),new RelativePriceIndicator(IndicatorPeriod.HOUR),new RelativePriceIndicator(IndicatorPeriod.DAY),
//                new RelativePriceIndicator(IndicatorPeriod.WEEK),
//        new RelativeVolumeIndicator(IndicatorPeriod.TENMINUTES),new RelativeVolumeIndicator(IndicatorPeriod.HOUR),new RelativeVolumeIndicator(IndicatorPeriod.DAY),
//                new RelativeVolumeIndicator(IndicatorPeriod.WEEK),
//                new HistoryMoodIndicator(true,IndicatorPeriod.DAY),new HistoryMoodIndicator(true,IndicatorPeriod.WEEK),
//                new HistoryMoodIndicator(false,IndicatorPeriod.DAY),new HistoryMoodIndicator(false,IndicatorPeriod.WEEK),
//                new MacdIndicator(26,12,9,0),new MacdIndicator(110,40,28,1),new MacdIndicator(200,50,35,2),
//                new DemaIndicator(26,12,9,0),new DemaIndicator(110,40,28,1),new DemaIndicator(200,50,35,2),
//                new MaxPriceChangeIndicator(IndicatorPeriod.TENMINUTES,true),new MaxPriceChangeIndicator(IndicatorPeriod.TENMINUTES,false),
//                new MaxPriceChangeIndicator(IndicatorPeriod.HOUR,true),new MaxPriceChangeIndicator(IndicatorPeriod.HOUR,false),
//                new MaxPriceChangeIndicator(IndicatorPeriod.DAY,true),new MaxPriceChangeIndicator(IndicatorPeriod.DAY,false),
//                new SignChangeIndicator(MacdIndicator.Id,1,true),new SignChangeIndicator(MacdIndicator.Id,1,false),
//                new SignChangeIndicator(MacdIndicator.Id,5,true),new SignChangeIndicator(MacdIndicator.Id,5,false),
//                new SignChangeIndicator(MacdIndicator.Id+1,1,true),new SignChangeIndicator(MacdIndicator.Id+1,1,false),
//                new SignChangeIndicator(MacdIndicator.Id+1,5,true),new SignChangeIndicator(MacdIndicator.Id+1,5,false),
//                new SignChangeIndicator(MacdIndicator.Id+2,1,true),new SignChangeIndicator(MacdIndicator.Id+2,1,false),
//                new SignChangeIndicator(MacdIndicator.Id+2,5,true),new SignChangeIndicator(MacdIndicator.Id+2,5,false),
//
//                new SignChangeIndicator(DemaIndicator.Id,1,true),new SignChangeIndicator(DemaIndicator.Id,1,false),
//                new SignChangeIndicator(DemaIndicator.Id,5,true),new SignChangeIndicator(DemaIndicator.Id,5,false),
//                new SignChangeIndicator(DemaIndicator.Id+1,1,true),new SignChangeIndicator(DemaIndicator.Id+1,1,false),
//                new SignChangeIndicator(DemaIndicator.Id+1,5,true),new SignChangeIndicator(DemaIndicator.Id+1,5,false),
//                new SignChangeIndicator(DemaIndicator.Id+2,1,true),new SignChangeIndicator(DemaIndicator.Id+2,1,false),
//                new SignChangeIndicator(DemaIndicator.Id+2,5,true),new SignChangeIndicator(DemaIndicator.Id+2,5,false),

//        };

        Arrays.sort(indicators, Comparator.comparingInt(IIndicator::getId));
        for (int i = 1;i<indicators.length;i++)
            if (indicators[i-1].getId()==indicators[i].getId())
                System.out.println(String.format("dublicate id %d %s %s", indicators[i].getId(),indicators[i].getName(),indicators[i-1].getName()));
        map = new IIndicator[Arrays.stream(indicators).mapToInt(IIndicator::getId).max().getAsInt()+1];
        for (IIndicator i : indicators)
            map[i.getId()] = i;
    }

    private IIndicator createIndicator(IndicatorInitData data) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        String c = "ru.gustos.trading.book.indicators."+data.name +"Indicator";
        Class cl = Class.forName(c);
        return (IIndicator) cl.getDeclaredConstructor(IndicatorInitData.class).newInstance(data);
    }

    public IIndicator[] listIndicators(){
        return indicators;
    }

    public IIndicator[] listIndicatorsShow(){
        return indicatorsShow;
    }

    public IIndicator get(int id) {
        return map[id];
    }
}

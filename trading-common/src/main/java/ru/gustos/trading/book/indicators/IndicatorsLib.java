package ru.gustos.trading.book.indicators;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class IndicatorsLib {
    private ArrayList<IIndicator> indicators = new ArrayList<>();
    private ArrayList<IIndicator> indicatorsShow = new ArrayList<>();
    private IIndicator[] map = new IIndicator[2000];

    public IndicatorsLib(String path) throws Exception {
        IndicatorInitData[] initData = new Gson().fromJson(FileUtils.readFileToString(new File(path)),IndicatorInitData[].class);


        for (int i = 0;i<initData.length;i++) {
            IIndicator ii = createIndicator(initData[i]);
            indicators.add(ii);
            if (ii.showOnPane())
                indicatorsShow.add(ii);
        }
        indicators.sort(Comparator.comparingInt(IIndicator::getId));
        indicatorsShow.sort(Comparator.comparingInt(IIndicator::getId));
        for (int i = 1;i<indicators.size();i++) {
            IIndicator ii = indicators.get(i);
            IIndicator ip = indicators.get(i - 1);
            if (ip.getId()== ii.getId())
                System.out.println(String.format("dublicate id %d %s %s", ii.getId(),ii.getName(),ip.getName()));
        }
        for (IIndicator i : indicators)
            map[i.getId()] = i;
    }

    public IndicatorsLib(){}

    public void add(String name, IndicatorType type, double[] data){
        int id = indicators.size()==0?1:indicators.get(indicators.size() - 1).getId() + 1;
        PrecalcedIndicator ii = new PrecalcedIndicator(id, name, type, data);
        indicators.add(ii);
        indicatorsShow.add(ii);
        map[ii.getId()] = ii;
    }

    public void add(IIndicator ii){
        indicators.add(ii);
        if (ii.showOnPane())
            indicatorsShow.add(ii);
        map[ii.getId()] = ii;
    }

    private IIndicator createIndicator(IndicatorInitData data) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        String c = "ru.gustos.trading.book.indicators."+data.name +"Indicator";
        Class cl = Class.forName(c);
        return (IIndicator) cl.getDeclaredConstructor(IndicatorInitData.class).newInstance(data);
    }

    public List<IIndicator> listIndicators(){
        return indicators;
    }

    public List<IIndicator> listIndicatorsShow(){
        return indicatorsShow;
    }

    public IIndicator get(int id) {
        return map[id];
    }
}

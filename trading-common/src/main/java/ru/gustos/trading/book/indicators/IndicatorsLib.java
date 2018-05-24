package ru.gustos.trading.book.indicators;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class IndicatorsLib {
    public ArrayList<Indicator> indicators = new ArrayList<>();
    public ArrayList<Indicator> indicatorsShow = new ArrayList<>();
    public ArrayList<Indicator> indicatorsShowBottom = new ArrayList<>();
    public ArrayList<Indicator> indicatorsBack = new ArrayList<>();
    public ArrayList<Indicator> indicatorsPrice = new ArrayList<>();
    public ArrayList<Indicator> indicatorsUnder = new ArrayList<>();
    private Indicator[] map = new Indicator[2000];

    public IndicatorsLib(String path) throws Exception {
        IndicatorInitData[] initData = new Gson().fromJson(FileUtils.readFileToString(new File(path)),IndicatorInitData[].class);


        for (int i = 0;i<initData.length;i++) {
            Indicator ii = createIndicator(initData[i]);
            indicators.add(ii);
            if (ii.show())
                indicatorsShow.add(ii);
        }
        indicators.sort(Comparator.comparingInt(Indicator::getId));
        for (int i = 1;i<indicators.size();i++) {
            Indicator ii = indicators.get(i);
            Indicator ip = indicators.get(i - 1);
            if (ip.getId()== ii.getId())
                System.out.println(String.format("dublicate id %d %s %s", ii.getId(),ii.getName(),ip.getName()));
        }
        for (Indicator i : indicators)
            map[i.getId()] = i;
        sortIndicators();
    }

    public IndicatorsLib(){}

    public void add(String name, IndicatorResultType type, double[] data){
        int id = indicators.size()==0?1:indicators.get(indicators.size() - 1).getId() + 1;
        PrecalcedIndicator ii = new PrecalcedIndicator(id, name, type, data);
        indicators.add(ii);
        indicatorsShow.add(ii);
        map[ii.getId()] = ii;
    }

    public void add(Indicator ii){
        indicators.add(ii);
        if (ii.show())
            indicatorsShow.add(ii);
        map[ii.getId()] = ii;
    }

    private Indicator createIndicator(IndicatorInitData data) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        String name = data.name;
        int ind = name.indexOf('$');
        if (ind>0)
            name = name.substring(0,ind);
        String c = "ru.gustos.trading.book.indicators."+ name +"Indicator";
        Class cl = Class.forName(c);
        return (Indicator) cl.getDeclaredConstructor(IndicatorInitData.class).newInstance(data);
    }

    public void sortIndicators(){
        indicatorsShow.clear();
        indicatorsShowBottom.clear();
        indicatorsBack.clear();
        indicatorsPrice.clear();
        indicatorsUnder.clear();
        for (Indicator i : indicators){
            if (i.showOnBottom())
                indicatorsShowBottom.add(i);
            if (i.show()) {
                indicatorsShow.add(i);
                if (i.getVisualType() == IndicatorVisualType.BACK)
                    indicatorsBack.add(i);
                if (i.getVisualType() == IndicatorVisualType.PRICELINE)
                    indicatorsPrice.add(i);
                if (i.getVisualType() == IndicatorVisualType.UNDERBARS)
                    indicatorsUnder.add(i);
                if (i.getVisualType() == IndicatorVisualType.UNDERLINE)
                    indicatorsUnder.add(i);
            }
        }
    }

    public Indicator get(int id) {
        return map[id];
    }
}

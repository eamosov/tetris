package ru.gustos.trading.book.indicators;

import kotlin.Pair;
import ru.gustos.trading.book.Sheet;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static ru.gustos.trading.book.indicators.IndicatorVisualType.BACK;
import static ru.gustos.trading.book.indicators.IndicatorVisualType.UNDERBARS;

public abstract class Indicator {
    public static final double YES = 1.0;
    public static final double NO = -1.0;

    public IndicatorInitData data;
    protected ArrayList<String> parameters = new ArrayList<>();
    public Indicator(IndicatorInitData data){
        this.data = data;
        if (data.t1!=0)
            parameters.add("t1");
        if (data.t2!=0)
            parameters.add("t2");
        if (data.t3!=0)
            parameters.add("t3");
        if (data.k1!=0)
            parameters.add("k1");
        if (data.k2!=0)
            parameters.add("k2");
        if (data.k3!=0)
            parameters.add("k3");
    }

    public int getId() {
        return data.id;
    }
    public String getName(){
        return data.name;
    }

    public boolean show(){
        return data.show;
    }

    public boolean showOnBottom(){
        return data.showOnBottom;
    }

    public IndicatorResultType getResultType() {return IndicatorResultType.YESNO;}
    public IndicatorVisualType getVisualType(){
        if (getResultType()==IndicatorResultType.YESNO)
            return BACK;
        else
            return UNDERBARS;
    }

    public ColorScheme getColors(){
        return ColorScheme.REDGREEN;
    }

    public Map<String,String> getMarks(int ind){ return null;}

    public int getNumberOfLines() { return 1;}
    public double getUpperBound(){
        return Double.MAX_VALUE;
    }
    public double getLowerBound(){
        return Double.MIN_VALUE;
    }
    public abstract void calcValues(Sheet sheet, double[][] values, int from, int to);

    public boolean fromZero() {
        return getLowerBound()==0;
    }

    @Override
    public String toString() {
        return getName();
    }

    public ArrayList<Pair<String,String>> getParameters(){
        ArrayList<Pair<String,String>> res = new ArrayList<>();
        for (String p : parameters){

            try {
                res.add(new Pair<>(p,IndicatorInitData.class.getField(p).get(data).toString()));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    public void setParameter(String name, String value){
        try {
            Field field = IndicatorInitData.class.getField(name);
            if (field.getType()==int.class)
                field.setInt(data,Integer.parseInt(value));
            else if (field.getType()==double.class)
                field.setDouble(data,Double.parseDouble(value));
            else if (field.getType()==String.class)
                field.set(data,value);
            else
                throw new NullPointerException("type not supported for "+name);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

    }
    public void setParameters(ArrayList<Pair<String,String>> params){
        for (Pair<String,String> p : params){
            setParameter(p.getFirst(),p.getSecond());
        }
    }

    public Object getCoreObject(){
        return null;
    }

}


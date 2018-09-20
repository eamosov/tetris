package ru.gustos.trading.global;

import ru.gustos.trading.ml.J48AttributeFilter;

import java.io.Serializable;
import java.util.HashSet;

public class MetaData implements Serializable {
    public int index;
    public String key;
    public boolean future;
    public boolean result;
    public int level;
    public boolean bool;
    public boolean data(HashSet<String> ignoreAttributes, J48AttributeFilter filter, int level){
        return !future && !result && this.level<=level && (ignoreAttributes==null || !ignoreAttributes.contains(key) && (filter==null || filter.isGood(index)));
    }
}

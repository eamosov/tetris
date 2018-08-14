package ru.gustos.trading.global;

import java.util.HashSet;

public class MetaData{
    public int index;
    public String key;
    public boolean future;
    public boolean result;
    public int level;
    public boolean bool;
    public boolean data(HashSet<String> ignoreAttributes, int level){
        return !future && !result && this.level<=level && !ignoreAttributes.contains(key);
    }
}

package ru.gustos.trading.global;

public class MetaData{
    public int index;
    public String key;
    public boolean future;
    public boolean result;
    public int level;
    public boolean bool;
    public boolean data(){
        return !future && !result;
    }

    public boolean data(int level){
        return !future && !result && this.level<=level;
    }
}

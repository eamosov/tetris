package ru.gustos.trading.global;

import java.util.ArrayList;
import java.util.Hashtable;

public class DecisionModel {
    static final String MAIN = "main";


    int index;
    boolean full = false;
    Hashtable<String, ArrayList> models = new Hashtable<>();
    Hashtable<String, ArrayList> models2 = new Hashtable<>();

    public DecisionModel() {
        initModels();
    }


    public DecisionModel(DecisionModel model, int index) {
        initModels();
        this.index = index;
    }

    private void initModels() {

        models.put(MAIN, new ArrayList<>());
        models2.put(MAIN, new ArrayList<>());
    }


    public void clear() {
        full = false;
        models.get(MAIN).clear();
        models2.get(MAIN).clear();

    }
}

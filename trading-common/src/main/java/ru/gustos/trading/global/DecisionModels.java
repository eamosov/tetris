package ru.gustos.trading.global;

import weka.classifiers.Evaluation;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;


public class DecisionModels{
    DecisionManager manager;
    public FilterMomentsModel model;
    FilterMomentsModel newModel = null;
    BuyMomentModel buymodel;
    BuyMomentModel newBuyModel = null;


    InstrumentData data;

    public DecisionModels(DecisionManager manager){
        this.manager = manager;
        data = manager.data;
        model = new FilterMomentsModel(manager);
        if (data.buydata!=null)
            buymodel = new BuyMomentModel(manager);

    }

    public void checkNeedRenew(boolean thread) {
        if (manager.calcIndex() >= manager.calcModelFrom && manager.calcIndex() - model.index >= 60 && manager.calcIndex() < manager.dontRenewAfter) {


            if (model.canRenewModel() || (buymodel!=null && buymodel.needRenew()))
                renewModel(thread);

        }
    }

    private void renewModel(boolean thread) {
        if (manager.LOGS)
            System.out.println("renew model on " + data.instrument + ", day " + (manager.calcIndex() / (60 * 24)));
        final int currentIndex = manager.calcIndex();
        final FilterMomentsModel model = new FilterMomentsModel(manager);
        final BuyMomentModel buymodel = data.buydata!=null?new BuyMomentModel(manager):null;
        if (thread)
            new Thread(() -> doRenewModel(model, buymodel, currentIndex)).start();
        else
            doRenewModel(model, buymodel, currentIndex);

    }

    private void doRenewModel(FilterMomentsModel model, BuyMomentModel buymodel, int calcIndex) {

        if (calcIndex >= manager.calcModelFrom) {
            prepareModel(model, buymodel, calcIndex);
        }

        synchronized (this) {
            newModel = model;
            newBuyModel = buymodel;
        }
    }

    void checkTakeNewModel() {
            if (newModel != null) {
                synchronized (this) {
                    model = newModel;
                    newModel = null;
                    buymodel = newBuyModel;
                    newBuyModel = null;
                    if (buymodel!=null)
                        buymodel.install();
                    manager.plhistoryClassifiedBuy.newModel(data.bar(manager.calcIndex() - 1).getBeginTime());
                }
            }
    }


    private void prepareModel(FilterMomentsModel model, BuyMomentModel buymodel, int calcIndex) {
        long endtime = data.bar(calcIndex - 1).getEndTime().toEpochSecond();
        try {
            if (buymodel!=null) {
                buymodel.prepare();
                buymodel.install();
            }
            model.clear();
            model.makeGoodBadModel(model, calcIndex, endtime, 9);
            model.correctNewModel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public boolean hasModel() {
        return model != null && model.classifier!=null;
    }
}


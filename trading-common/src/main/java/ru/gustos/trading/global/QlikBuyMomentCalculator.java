package ru.gustos.trading.global;

public class QlikBuyMomentCalculator implements BuyMomentCalculator {
    DecisionManager manager;
    public QlikBuyMomentCalculator(DecisionManager manager){
        this.manager = manager;
    }

    @Override
    public boolean shouldBuy() {

        return manager.models.buymodel.shouldBuy();
    }
}

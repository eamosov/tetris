package ru.gustos.trading.global;

public class GustosBuySellMomentCalculator implements BuyMomentCalculator, SellMomentCalculator {
    DecisionCalc calc;
    public GustosBuySellMomentCalculator(DecisionCalc calc){
        this.calc = calc;
    }

    @Override
    public boolean shouldBuy() {
        return CalcUtils.gustosBuy(calc.data,calc.calcIndex,calc.values.gustosAvg,calc.values.gustosParams);
    }

    @Override
    public boolean shouldSell() {
        return CalcUtils.gustosSell(calc.data,calc.calcIndex,calc.values.gustosAvg4,calc.values.gustosParams);
    }

}


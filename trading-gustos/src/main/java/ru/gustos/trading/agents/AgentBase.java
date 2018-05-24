package ru.gustos.trading.agents;

import ru.gustos.trading.book.indicators.Indicator;

import java.util.Arrays;

public abstract class AgentBase {
    public static final double fee = 0.0005;
    AgentManager manager;
    AgentState agentState;

    double[] decisions;


    public AgentBase(AgentManager manager) {
        this.manager = manager;
        decisions = new double[manager.sheet.size()];
        initStartParams();

    }

    protected void initStartParams() {
        agentState = new AgentState();
        agentState.money = 1000;
        agentState.btc = 0;
    }

    protected void sell(int index) {
        int buyIndex = agentState.lastOp;
        agentState.sell(index);
        Arrays.fill(decisions,buyIndex,index, agentState.sellCost>agentState.buyCost? Indicator.YES: Indicator.NO);
    }

    protected void buy(int index) {
        agentState.buy(index);
    }


    public abstract void tick(int index);

    public class AgentState {
        double money;
        double btc;

        int lastOp;

        int buys;

        double buyCost;
        double sellCost;

        protected void buy(int index){
            lastOp = index;
            buyCost = manager.sheet.bar(index).getClosePrice() * (1 + fee);
            btc+=money/ buyCost;
            money = 0;
            buys++;
        }

        protected  void sell(int index){
            lastOp = index;
            sellCost = manager.sheet.bar(index).getClosePrice()*(1-fee);
            money+=btc*sellCost;
            btc = 0;
        }

        public double full(int index) {
            return money+btc*manager.sheet.bar(index).getClosePrice()*(1-fee);
        }
    }
}



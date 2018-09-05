package ru.gustos.trading.global;

import ru.efreet.trading.bars.XBar;
import weka.core.Instance;
import weka.core.Instances;

import static ru.gustos.trading.global.DecisionManager.calcAllFrom;

public class BuyMomentModel {
    DecisionManager manager;
    InstrumentData data;
    int index;
    GustosBranches branch;

    public BuyMomentModel(DecisionManager manager) {
        this.manager = manager;
        data = manager.data;
        index = manager.calcIndex()-1;
    }


    public boolean shouldBuy() {
        return shouldBuy(manager.calcIndex());
    }

    public boolean shouldBuy(int index) {
        if (manager.data.buyhelper.futureAttributes()==0 || branch==null) return false;
        Instance inst = manager.data.buyhelper.makeInstance(manager.data.buydata.get(index), null, null, 0, 9);
        inst.setDataset(manager.data.buyhelper.makeEmptySet(null, null, 0, 9));
        return branch.check(inst);
    }

    public void prepare() throws Exception {
        branch = new GustosBranches();
        long time = manager.data.bar(index).getEndTime().toEpochSecond();
        int validFrom = index - 24 * 60 * 5;
        Instances train = manager.data.buyhelper.makeSet(manager.data.buydata(), null, null, Math.max(calcAllFrom, index - 90 * 24 * 60), validFrom, time, 0, 9, 0);
        branch.build(train, 10, 100);
        Instances valid = manager.data.buyhelper.makeSet(manager.data.buydata(), null, null, validFrom, index, Long.MAX_VALUE, 0, 9, 0);
        branch.limit = bestLimitBuy(valid,validFrom,branch,10);
    }

    public void install() {
        for (int i = calcAllFrom;i<data.size();i++)
            data.buys.set(i, shouldBuy(i));
        manager.calc.prepareGoodBuy();
        manager.calc.prepareGoodSell(false);
        manager.calc.prepareGustosProfit();

    }

    private int bestLimitBuy(Instances set, int from, GustosBranches buy, int maxtrades) {
        int best = 1;
        double profit = 1;
//        System.out.println("find limit for buy:");
        for (int i = 2; i < 200; i++) {
            PLHistory h = new PLHistory(manager.data.instrument.toString(), null);
            for (int j = 0; j < set.size(); j++) {
                int index = from + j;
                XBar bar = manager.data.bars.get(index);
                if (manager.data.sells.get(index)) {
                    h.sellMoment(bar.getClosePrice(), bar.getEndTime().toEpochSecond());
                } else if (buy.check(set.get(j), 0, i, 1))
                    h.buyMoment(bar.getClosePrice(), bar.getEndTime().toEpochSecond());

            }
//            System.out.println(i + ")" + h.all);
            if (h.all.profit > profit && h.all.count <= maxtrades) {
                profit = h.all.profit;
                best = i;
            }
            if (h.all.count == 0) break;
        }
//        System.out.println(String.format("best: %d (profit %.4g)", best, profit));
        return best<4?30:best;
    }


    public boolean needRenew() {
        return false;
    }
}

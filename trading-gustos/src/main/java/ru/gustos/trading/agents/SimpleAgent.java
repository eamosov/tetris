package ru.gustos.trading.agents;

import kotlin.Pair;
import ru.efreet.trading.trainer.GdmBotTrainer;
import ru.efreet.trading.trainer.FloatBotMetrica;
import ru.efreet.trading.utils.PropertyEditorFactory;
import ru.gustos.trading.book.SheetUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SimpleAgent extends AgentBase{


    SimpleAgentProperties properties = new SimpleAgentProperties();


    public SimpleAgent(AgentManager manager, int index) {
        super(manager);
        do {
            properties.random(manager.random);
            optimize();
        } while (estimate(properties)<150);
        System.out.println("agent created");
    }

    private void optimize() {
        PropertyEditorFactory pp = properties.makePropertyEditorFactory();

        //Начальное множество параметров - исходных точек оптимизации
        List<SimpleAgentProperties> origin = new ArrayList<>();
        origin.add(new SimpleAgentProperties(properties));

        GdmBotTrainer<SimpleAgentProperties, Double, FloatBotMetrica> trainer = new GdmBotTrainer<SimpleAgentProperties, Double, FloatBotMetrica>();
        trainer.logs = false;
//        Pair<SimpleAgentProperties, Double> best = trainer.getBestParams(
//                pp.getGenes(),
//                origin, // исходные точки
//                p -> {  // функция, которая для каждой исходной точки подсчитвает результат (любого типа)
//                    return estimate(p);
//                },
//                (p, r) ->  { //функция, которая для пары (точка,результат) подсчитывает метрику, которая максимизируется
//                    return new DoubleBotMetrica(r);
//                },
//                p -> {  //функция копирования точек
//                    return new SimpleAgentProperties(p);
//                },
//                (p, r) -> { //Коллбек, когда найден новый лучший кандидат(для отслеживания процесса)
////                    System.out.println("NEW: " + p.toString() + " " + r.toString());
//                    return  null;
//                });
//        properties = best.getFirst();
//        System.out.println("optimize "+best.getSecond());

    }

    private double estimate(SimpleAgentProperties p) {
        int ops = 0;
        AgentState a = new AgentState();
        a.money = 1000;
        a.btc = 0;
        int tick = manager.index-60*24*7;
        while (tick<manager.index){
            a.money *= 0.9998;
            a.btc *= 0.9998;
            if (operate(tick,a,p,false))
                ops++;
            tick++;
        }
        a.sell(tick);
        int div = Math.max(1,20 - ops);
        return a.money/ (div*div);
    }


    @Override
    public void tick(int index) {
        if (operate(index, agentState,properties,true))
            System.out.println("operation");
        double full = agentState.full(index);
        if (full>500 && manager.random.nextInt((int)(full -300)*10)==0) optimize();
    }

    protected boolean operate(int index, AgentState agentState, SimpleAgentProperties properties, boolean real){
        if (agentState.money>0){
            int len = Math.min(properties.buyHistory,index-agentState.lastOp);
            Pair<Double, Double> mm = SheetUtils.getMinMaxClose(manager.sheet, index - len, index);
            double v = manager.sheet.bar(index).getClosePrice()/mm.getFirst();
            if (v>properties.buyLo){// && v<properties.buyHi) {
                if (real)
                    buy(index);
                else
                    agentState.buy(index);
                return true;
            }
        }

        if (agentState.btc>0){
            int len = Math.min(properties.sellHistory,index-agentState.lastOp);
            Pair<Double, Double> mm = SheetUtils.getMinMaxClose(manager.sheet, index - len, index);
            double v = mm.getSecond() / manager.sheet.bar(index).getClosePrice();
            if (v>properties.sellLo){// && v<properties.sellHi) {
                if (real)
                    sell(index);
                else
                    agentState.sell(index);
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("(%d %d, %s)", (int)agentState.full(manager.index),agentState.buys,properties.toString());
    }

    public class SimpleAgentProperties{
        public double buyLo = 1.001;
        public double buyHi = 1.003;
        public int buyHistory = 30;

        public double sellLo = 1.001;
        public double sellHi = 1.003;
        public int sellHistory = 30;

        public SimpleAgentProperties() {
        }

        public SimpleAgentProperties(SimpleAgentProperties p) {
            buyLo = p.buyLo;
            buyHi = p.buyHi;
            buyHistory = p.buyHistory;

            sellLo = p.sellLo;
            sellHi = p.sellHi;
            sellHistory = p.sellHistory;
        }

        public void random(Random r){
            buyLo = 1.0+r.nextInt(50)*0.0002;
            buyHi = buyLo+100;//r.nextInt(500)*0.0002;
            sellLo = 1.0+r.nextInt(50)*0.0002;
            sellHi = sellLo+100;//r.nextInt(500)*0.0002;
            buyHistory = 3+r.nextInt(30);
            sellHistory = 3+r.nextInt(30);
        }

        @Override
        public String toString() {
            return String.format("(buy: %d, %.6g)", buyHistory,buyLo);
        }

        public PropertyEditorFactory makePropertyEditorFactory() {
            PropertyEditorFactory<SimpleAgentProperties> p = PropertyEditorFactory.of(SimpleAgentProperties.class, SimpleAgentProperties::new);

            p.of(Double.class, "buyLo", "buyLo", 1.0, 1.05, 0.00005, true);
//            p.of(Double.class, "buyHi", "buyHi", 1, 1.3, 0.0001, false);
            p.of(Integer.class, "buyHistory", "buyHistory", 3, 60, 1, true);
            p.of(Double.class, "sellLo", "sellLo", 1.0, 1.05, 0.00005, true);
//            p.of(Double.class, "sellHi", "sellHi", 1, 1.3, 0.0001, false);
            p.of(Integer.class, "sellHistory", "sellHistory", 3, 60, 1, true);
            return p;

        }
    }
}

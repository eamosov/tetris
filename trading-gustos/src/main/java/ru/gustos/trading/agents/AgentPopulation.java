package ru.gustos.trading.agents;

import ru.gustos.trading.book.indicators.VecUtils;

import java.util.ArrayList;
import java.util.Arrays;

public class AgentPopulation {
    AgentManager manager;

    ArrayList<AgentBase> agents = new ArrayList<>();
    int removed;

    public AgentPopulation(AgentManager manager) {
        this.manager = manager;
    }

    public void add(AgentBase agentBase) {
        agents.add(agentBase);
    }

    public void tick(int index) {
        for (AgentBase a : agents)
            a.tick(index);

        int cnt = agents.size();
        agents.removeIf(a->a.agentState.full(index)<500);
        removed = agents.size()-cnt;
//        if (manager.random.nextInt(100)==0)
    }

    public void saveData() throws Exception {
        String path = "D:\\tetrislibs\\agents\\";
        for (int i = 0;i<agents.size();i++) {
            AgentBase a = agents.get(i);
            VecUtils.toFile(path+i,a.decisions);
        }
    }

    @Override
    public String toString() {
        double min = agents.stream().mapToDouble(a->a.agentState.full(manager.index)).min().getAsDouble();
        double max = agents.stream().mapToDouble(a->a.agentState.full(manager.index)).max().getAsDouble();
        double sum = agents.stream().mapToDouble(a->a.agentState.full(manager.index)).sum();
        return String.format("size: %d, removed: %d, min: %d, max: %d, sum: %d, list %s", agents.size(),removed, (int)min, (int)max, (int)sum, agents.toString());
    }
}

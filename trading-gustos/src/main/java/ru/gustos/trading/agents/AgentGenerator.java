package ru.gustos.trading.agents;

public class AgentGenerator {
    AgentManager manager;

    public AgentGenerator(AgentManager manager) {
        this.manager = manager;

    }

    public AgentBase makeNew(int startIndex) {
        return new SimpleAgent(manager,startIndex);
    }
}

package ru.gustos.trading.agents;

import ru.gustos.trading.book.Sheet;

import java.util.Random;

public class AgentManager {
    Sheet sheet;

    AgentGenerator generator;
    AgentPopulation population;

    Random random;

    int index;

    public AgentManager(Sheet sheet){
        this.sheet = sheet;
        random = new Random();
        generator = new AgentGenerator(this);
        population = new AgentPopulation(this);
    }

    public void init(int startCount, int startIndex){
        index = startIndex;
        while (startCount-->0){
            population.add(generator.makeNew(index));
        }
    }

    public void iterateBar(){
        index++;
        population.tick(index);
    }

    public void runTillEnd(){
        while (index<sheet.moments.size()-1) {
            iterateBar();
            if (index%60==0)
                System.out.println("population: "+population.toString());
        }
        try {
            population.saveData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

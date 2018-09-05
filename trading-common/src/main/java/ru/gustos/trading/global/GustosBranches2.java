package ru.gustos.trading.global;

import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.HashSet;

public class GustosBranches2{

    Instances set;
    ArrayList<RandomTreeWithExam.Branch> branchesTrue;
    ArrayList<RandomTreeWithExam.Branch> branchesFalse;
    public int limit;
    int cpus;

    public GustosBranches2() {
        this(8);
    }

    public GustosBranches2(int cpus) {
        this.cpus = cpus;
    }

}

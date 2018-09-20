package ru.gustos.trading.global;

public class ProfitPredict {
    static final int skip = 500;
    static final int train = 1000;
    ExperimentData experimentData;
    InstrumentData data;

    public ProfitPredict(ExperimentData experimentData, InstrumentData data){
        this.experimentData = experimentData;
        this.data = data;
    }

    public void fill(){
//        data.helper.makeSet(data.bars.data.stream().map(m->m.mldata).toArray(MomentDataProvider[]::new),skip,skip+train);

    }


}

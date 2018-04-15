package ru.gustos.trading.book.indicators;

public abstract class BaseIndicator implements IIndicator{
    int id;
    boolean show;

    public BaseIndicator(IndicatorInitData data){

        id = data.id;
        show = data.show;
    }

    public int getId() {        return id;    }

    @Override
    public boolean showOnPane() {
        return show;
    }
}

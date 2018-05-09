package ru.gustos.trading.book.indicators;

import java.util.Properties;

public interface IIndicatorWithProperties{
    Properties getIndicatorProperties();
    void setIndicatorProperties(Properties p);
}

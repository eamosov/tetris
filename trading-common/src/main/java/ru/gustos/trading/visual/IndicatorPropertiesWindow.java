package ru.gustos.trading.visual;

import ru.gustos.trading.book.indicators.Indicator;
import ru.gustos.trading.book.indicators.IIndicatorWithProperties;

import javax.swing.*;
import java.awt.*;
import java.util.Hashtable;
import java.util.Properties;

public class IndicatorPropertiesWindow extends JFrame {
    Visualizator vis;
    Indicator indicator;
    Hashtable<String,JTextField> data = new Hashtable<>();
    public IndicatorPropertiesWindow(Visualizator vis, Indicator indicator){
        super("Logic properties "+indicator.getName());
        this.vis = vis;
        this.indicator = indicator;
        Properties props = ((IIndicatorWithProperties) indicator).getIndicatorProperties();
        init(props);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setVisible(true);
    }

    private void init(Properties props) {
        Container pane = getContentPane();
        pane.setLayout(new GridLayout(props.size()+1,2));
        for (Object key : props.keySet()){
            pane.add(new JLabel(key.toString()));
            JTextField text = new JTextField();
            data.put((String)key,text);
            text.setText(props.get(key).toString());
            pane.add(text);
        }
        pane.add(new JLabel());
        JButton apply = new JButton("Apply");
        pane.add(apply);
        apply.addActionListener(e -> apply());
    }

    private void apply() {
        Properties p = new Properties();
        for (String key : data.keySet())
            p.put(key,data.get(key).getText());
        ((IIndicatorWithProperties) indicator).setIndicatorProperties(p);
//        vis.getSheet().calcIndicators();
        vis.fireViewUpdated();
    }
}

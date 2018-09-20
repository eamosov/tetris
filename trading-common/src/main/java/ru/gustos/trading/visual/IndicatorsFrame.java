package ru.gustos.trading.visual;

import kotlin.Pair;
import ru.gustos.trading.book.indicators.Indicator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.function.Consumer;

public class IndicatorsFrame extends JFrame {
    Visualizator vis;

    private JList<Indicator> list;
    private IndicatorOptionsPane options;

    public IndicatorsFrame(Visualizator vis){
        this.vis = vis;

        Container cp = getContentPane();
        setSize(550,300);

        list = new JList<>();
        options = new IndicatorOptionsPane();

        list.setModel(new DefaultListModel<Indicator>());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(l->options.set(list.getSelectedValue()));
        fillList();
        cp.add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,list,options),BorderLayout.CENTER);

        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setVisible(true);
    }

    private void fillList() {
//        for (Indicator ind : vis.getSheet().getLib().indicators)
//            ((DefaultListModel<Indicator>)list.getModel()).addElement(ind);
    }


    class IndicatorOptionsPane extends JPanel {
        JCheckBox show = new JCheckBox();
        JCheckBox showOnBottom = new JCheckBox();
        Indicator indicator;
        IndicatorOptionsPane(){
            show.addActionListener(a->onShow());
            showOnBottom.addActionListener(a->onShowOnBottom());
        }

        private void onShowOnBottom() {
            if (indicator!=null)
                vis.setIndicatorShowOnBottom(indicator,showOnBottom.isSelected());

        }

        private void onShow() {
            if (indicator!=null)
                vis.setIndicatorShow(indicator,show.isSelected());
        }

        Hashtable<String, JTextField> indicatorParams = new Hashtable<>();
        Hashtable<String, String> oldValue = new Hashtable<>();
        void set(Indicator indicator) {
            this.indicator = null;
            show.setSelected(indicator.show());
            showOnBottom.setSelected(indicator.showOnBottom());
            removeAll();
            ArrayList<Pair<String, String>> params = indicator.getParameters();

            setLayout(new GridLayout(Math.max(5+params.size(),8), 2));
            add(new JLabel("Name",SwingConstants.CENTER));
            add(new JLabel(indicator.getName()));
            add(new JLabel("Show",SwingConstants.CENTER));
            add(show);
            add(new JLabel("On bottom", SwingConstants.CENTER));
            add(showOnBottom);

            indicatorParams.clear();
            for (Pair<String,String> p : params){
                add(new JLabel(p.getFirst(), SwingConstants.CENTER));
                JTextField txt = new JTextField(p.getSecond());
                add(txt);
                indicatorParams.put(p.getFirst(),txt);
                oldValue.put(p.getFirst(),p.getSecond());
                txt.addActionListener(l->{
                    updateParams();
                });
                txt.addFocusListener(new FocusListener() {
                    @Override
                    public void focusGained(FocusEvent e) {
                    }
                    @Override
                    public void focusLost(FocusEvent e) {
                        updateParams();
                    }
                });

            }
            this.indicator = indicator;
            revalidate();
            repaint();
        }

        void updateParams(){
            boolean changed = false;
            for (String key : indicatorParams.keySet()) {
                String old = oldValue.get(key);
                String now = indicatorParams.get(key).getText().trim();
                if (!old.equals(now)) {
                    indicator.setParameter(key, now);
                    oldValue.put(key,now);
                    changed = true;
                }
            }
            if (changed) {
//                vis.getSheet().getData().calc(indicator);
                vis.fireViewUpdated();
            }
        }

    }
}

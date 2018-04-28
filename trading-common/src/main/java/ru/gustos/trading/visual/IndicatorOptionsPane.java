package ru.gustos.trading.visual;

import kotlin.Pair;
import ru.gustos.trading.book.indicators.BaseIndicator;
import ru.gustos.trading.book.indicators.IIndicator;
import ru.gustos.trading.book.indicators.IStringPropertyHolder;
import ru.gustos.trading.book.indicators.IndicatorsLib;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class IndicatorOptionsPane extends JPanel {
    Visualizator vis;
    List<Pair<IIndicator,JCheckBox>> list = new ArrayList<>();

    public IndicatorOptionsPane(Visualizator vis){
        this.vis = vis;
        IndicatorsLib lib = vis.getSheet().getLib();
        for(IIndicator ii : lib.listIndicators())
            if (ii.priceLine()){
                final JCheckBox check = new JCheckBox();
                add(check);
                check.addActionListener(e->{updateProperties();});
                add(new JLabel(ii.getName()));
                add(new JLabel("        "));
                list.add(new Pair<>(ii,check));
            } else if (ii instanceof IStringPropertyHolder){
                final IStringPropertyHolder h = (IStringPropertyHolder)ii;
                add(new JLabel(h.getPropertyName()));
                final IIndicator indicator = ii;
                final JTextField txt = new JTextField(h.getPropertyValue());
                txt.setColumns(15);
                add(txt);
                txt.addActionListener(e->{
                    h.setPropertyValue(txt.getText());
                    vis.getSheet().getData().calc(indicator);
                    vis.fireViewUpdated();
                });
                add(new JLabel("        "));
            }

    }

    private void updateProperties() {
        for (int i = 0;i<list.size();i++){
            Pair<IIndicator, JCheckBox> p = list.get(i);
            if (p.getFirst() instanceof BaseIndicator)
                ((BaseIndicator)p.getFirst()).setShow(p.getSecond().isSelected());

        }
        vis.fireViewUpdated();
    }

}

package ru.gustos.trading.visual;

import javax.swing.*;
import java.awt.*;

public class TimelinePanel extends JPanel {
    private Visualizator vis;

    public TimelinePanel(Visualizator vis) {
        this.vis = vis;
        Dimension d = getPreferredSize();
        d.height = 80;
        setPreferredSize(d);
    }

    public void paint(Graphics g){
        super.paint(g);
    }



}

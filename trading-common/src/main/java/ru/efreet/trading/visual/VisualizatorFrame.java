package ru.efreet.trading.visual;

import ru.efreet.trading.book.Sheet;

import javax.swing.*;

public class VisualizatorFrame extends JFrame{
    Visualizator vis;
    VisualizatorForm form;

    public VisualizatorFrame(Visualizator vis){
        super("Trade");
        this.vis = vis;
        form = new VisualizatorForm(vis);
        setContentPane(form.getRoot());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1600,800);
    }

}

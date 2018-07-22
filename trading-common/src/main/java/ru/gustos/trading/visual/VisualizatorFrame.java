package ru.gustos.trading.visual;

import javafx.scene.input.KeyCode;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class VisualizatorFrame extends JFrame{
    Visualizator vis;
    VisualizatorForm form;
    JMenuBar menu = new JMenuBar();

    public VisualizatorFrame(Visualizator vis){
        super("Trade");
        this.vis = vis;
        initMenu();
        setJMenuBar(menu);
        form = new VisualizatorForm(vis);
        setContentPane(form.getRoot());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1600,800);
        form.getRoot().setFocusable(true);
        form.getRoot().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("X"),"next");
        form.getRoot().getActionMap().put("next", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                vis.right();

            }
        });
        form.getRoot().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("Z"),"prev");
        form.getRoot().getActionMap().put("prev", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                vis.left();

            }
        });
    }

    private void initMenu() {
        JMenu mm = new JMenu("Menu");
        menu.add(mm);
        JMenu mm2 = new JMenu("Right");
        menu.add(mm2);

        JMenuItem indicators = new JMenuItem("Indicators");
        indicators.addActionListener(a->vis.onShowIndicators());
        mm.add(indicators);

        JMenuItem run = new JMenuItem("Run");
        run.addActionListener(a->vis.runPlay());
        mm.add(run);

        JCheckBoxMenuItem fullZoom = new JCheckBoxMenuItem("Full zoom");
        fullZoom.setState(vis.getFullZoom());
        fullZoom.addActionListener(a->vis.setFullZoom(fullZoom.getState()));
        mm.add(fullZoom);

        JCheckBoxMenuItem priceLine = new JCheckBoxMenuItem("Price line");
        priceLine.setState(vis.getPriceLineByClick());
        priceLine.addActionListener(a->vis.setPriceLineByClick(priceLine.getState()));
        mm.add(priceLine);

        JCheckBoxMenuItem localModel = new JCheckBoxMenuItem("Local model");
        localModel.setState(vis.getLocalModelByClick());
        localModel.addActionListener(a->vis.setLocalModelByClick(localModel.getState()));
        mm.add(localModel);

        JCheckBoxMenuItem fixedVolume = new JCheckBoxMenuItem("Fixed volumes");
        fixedVolume.setState(vis.getFixedVolumes());
        fixedVolume.addActionListener(a->vis.setFixedVolumes(fixedVolume.getState()));
        mm2.add(fixedVolume);

        JCheckBoxMenuItem gustosVolume = new JCheckBoxMenuItem("Gustos volumes");
        gustosVolume.setState(vis.getGustosVolumes());
        gustosVolume.addActionListener(a->vis.setGustosVolumes(gustosVolume.getState()));
        mm2.add(gustosVolume);
    }

}

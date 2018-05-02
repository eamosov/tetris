package ru.gustos.trading.visual;

import ru.gustos.trading.bots.*;

import javax.swing.*;
import java.awt.event.*;

public class RunBotDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JPanel centerPanel;
    private JPanel bottomPanel;
    private JRadioButton fromStart;
    private JRadioButton fromHere;
    private JList botList;

    private Visualizator vis;

    public RunBotDialog(Visualizator vis) {
        this.vis = vis;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        fromStart.setSelected(true);
        ButtonGroup group = new ButtonGroup();
        group.add(fromStart);
        group.add(fromHere);
        DefaultListModel model = new DefaultListModel();
        botList.setModel(model);
        model.addElement("Oracle bot");
        model.addElement("EfreetIndicator bot");
        model.addElement("Y bot");
        model.addElement("Random bot");
        model.addElement("Check period 15m");
        model.addElement("Check period 2h");
        botList.setSelectedIndex(0);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        boolean here = fromHere.isSelected();
        String botName = (String)botList.getSelectedValue();
        BotRunner r = new BotRunner();
        IDecisionBot bot = null;
        if (botName.startsWith("Oracle"))
            bot = new OracleBot();
        else if (botName.startsWith("EfreetIndicator"))
            bot = new EfreetBot();
        else if (botName.startsWith("Random"))
            bot = new RandomBot();
        else if (botName.startsWith("Check period 15m"))
            bot = new CheckPeriodBot(15,true);
        else if (botName.startsWith("Check period 2h"))
            bot = new CheckPeriodBot(120,true);

        double result = r.run(vis.getSheet(), here ? vis.getIndex() : 0, vis.getSheet().moments.size(), bot);
        dispose();
        JOptionPane.showMessageDialog(null,String.format("money x %1$,.2f",(result/BotRunner.startMoney)));
        new BotIntervalsVisualizer(vis.getSheet(),r.intervals);
    }

    private void onCancel() {
        dispose();
    }
}

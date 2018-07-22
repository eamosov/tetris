package ru.gustos.trading.visual;

import org.apache.commons.io.FileUtils;
import ru.gustos.trading.book.indicators.VecUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class SpecialPoints extends JFrame {
    double[] data;
    double max;
    double p = 6;
    double p2 = 6;

    SpecialPoints(double[] dd){
        super("special points");
        data = dd;
        max = Arrays.stream(data).max().getAsDouble();
        JPanel params = new JPanel();
        JTextField param = new JTextField(Double.toString(p));
        param.setColumns(8);
        params.add(param);
        JTextField param2 = new JTextField(Double.toString(p2));
        param2.setColumns(8);
        params.add(param2);
        getContentPane().add(params,BorderLayout.NORTH);
        final JScrollPane scroll = new JScrollPane();
        getContentPane().add(scroll,BorderLayout.CENTER);
        scroll.getViewport().add(new GraphPane());
        setSize(1800,700);
        setVisible(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        param.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                p = Double.parseDouble(param.getText());
                repaint();
            }
        });
        param2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                p2 = Double.parseDouble(param2.getText());
                repaint();
            }
        });
    }



    class GraphPane extends JPanel {
        int x;
        public GraphPane(){
            setPreferredSize(new Dimension(data.length,300));
            addMouseMotionListener(new MouseMotionListener() {
                @Override
                public void mouseDragged(MouseEvent e) {

                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    x = e.getX();
                    repaint();
                }
            });
        }

        public void paint(Graphics g){
            super.paint(g);
            double scale = 1;
            double k = p;
            double[] d = VecUtils.ma(VecUtils.hairFilter(VecUtils.hairFilter(data)),(int)(2*p2));
            double[] dif1 = VecUtils.dif(d);
            dif1 = VecUtils.ma(dif1,(int)p2);
            dif1 = VecUtils.hairFilter(VecUtils.hairFilter(dif1));
            dif1 = VecUtils.negativeHairFilter(VecUtils.negativeHairFilter(dif1));
            double[] difLongMa = VecUtils.ma(VecUtils.abs(dif1), 100);
//        double maxdif = Arrays.stream(dif).map(Math::abs).max().getAsDouble();
            dif1 = VecUtils.div(dif1,difLongMa);
//            dif1 = VecUtils.pitsToZero(dif1,2);
//            dif1 = VecUtils.smallToZero(dif1,0.4);
            dif1 = VecUtils.removeSmallsOnDif(dif1,2* k);
            double[] dif = dif1;
            double maxdif = Arrays.stream(dif).map(Math::abs).max().getAsDouble();
            int hh = getHeight()/2;
            for (int i = 0; i< d.length; i++){
                g.setColor(CandlesPane.GREEN);
                int h = (int)(Math.min(max,d[i]*scale)/max* hh);
                g.fillRect(i, hh -h,1,h);

                h  = (int)(Math.min(maxdif,dif[i]*scale)/maxdif*hh/2);
                if (h>0)
                    g.fillRect(i,hh+hh/2-h,1,h);
                else
                    g.fillRect(i,hh+hh/2,1,-h);

                g.setColor(new Color(0,0,0,64));
                h = (int)(Math.min(max,data[i]*scale)/max* hh);
                g.fillRect(i, hh -h,1,h);

            }

            g.setColor(Color.darkGray);
            g.drawLine(0,hh,getWidth(),hh);
            g.drawLine(x,0,x,getHeight());
            int[] levels = VecUtils.listLevels(data,p,p2);
            for (int ii : levels){
                g.drawLine(ii,0,ii,getHeight());
            }

        }
    }


    public static void main(String[] args) throws IOException {
        String s = FileUtils.readFileToString(new File("d:\\delme\\sum"));
        s = s.replace("[","");
        s = s.replace("]","");
        s = s.replace(", ",",");
        while (s.endsWith(",0.0")) s = s.substring(0,s.length()-4);
        double[] dd = Arrays.stream(s.split(",")).skip(4000).mapToDouble(Double::parseDouble).toArray();

        new SpecialPoints(dd);
    }
}

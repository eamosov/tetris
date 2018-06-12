package ru.gustos.trading.visual;

import org.apache.commons.io.FileUtils;
import ru.gustos.trading.book.indicators.VecUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SpecialPoints extends JFrame {
    double[] data;
    double max;

    SpecialPoints(double[] dd){
        super("special points");
        data = dd;
        max = Arrays.stream(data).max().getAsDouble();
        final JScrollPane scroll = new JScrollPane();
        getContentPane().add(scroll,BorderLayout.CENTER);
        scroll.getViewport().add(new GraphPane());
        setSize(1800,700);
        setVisible(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
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
            double k = 6;
            double[] dif = VecUtils.makeDifForLevels(data,k);
            double[] d = VecUtils.ma(VecUtils.hairFilter(VecUtils.hairFilter(data)),4);
            double maxdif = Arrays.stream(dif).map(Math::abs).max().getAsDouble();
            int hh = getHeight()/2;
            g.setColor(CandlesPane.GREEN);
            for (int i = 0; i< d.length; i++){
                int h = (int)(Math.min(max,d[i]*scale)/max* hh);
                g.fillRect(i, hh -h,1,h);

                h  = (int)(Math.min(maxdif,dif[i]*scale)/maxdif*hh/2);
                if (h>0)
                    g.fillRect(i,hh+hh/2-h,1,h);
                else
                    g.fillRect(i,hh+hh/2,1,-h);
            }

            g.setColor(Color.black);
            g.drawLine(0,hh,getWidth(),hh);
            g.drawLine(x,0,x,getHeight());
            List<Integer> levels = VecUtils.listLevels(data,k);
            for (Integer ii : levels){
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

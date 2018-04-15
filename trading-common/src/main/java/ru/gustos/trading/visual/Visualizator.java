package ru.gustos.trading.visual;

import ru.gustos.trading.book.Sheet;
import ru.gustos.trading.book.SheetUtils;

import javax.swing.event.EventListenerList;
import java.awt.*;
import java.time.ZonedDateTime;
import java.util.Arrays;

public class Visualizator {
    private Sheet sheet;
    private int from;
    private int zoom;
    private VisualizatorFrame frame;
    protected EventListenerList viewListeners = new EventListenerList();

    public Visualizator(Sheet sheet){
        this.sheet = sheet;
        frame = new VisualizatorFrame(this);
        frame.setVisible(true);
    }

    public void addListener(VisualizatorViewListener listener){
        viewListeners.add(VisualizatorViewListener.class,listener);
    }

    public void addListener(VisualizatorMouseListener listener){
        viewListeners.add(VisualizatorMouseListener.class,listener);
    }

    private void fireViewUpdated(){
        Arrays.stream(viewListeners.getListeners(VisualizatorViewListener.class)).forEach(VisualizatorViewListener::visualizatorViewChanged);
    }

    public void setFrom(ZonedDateTime from){
        this.from = sheet.getBarIndex(from);
        fireViewUpdated();
    }

    public void goLeft() {
        from-=barsOnScreen();
        fixFrom();
        fireViewUpdated();
    }

    public void goRight() {
        from+=barsOnScreen();
        fixFrom();
        fireViewUpdated();
    }

    public int getIndex() {
        return from;
    }

    public void setIndex(int from){
        this.from = from;
        fixFrom();
        fireViewUpdated();
    }

    private int barsOnScreen(){
        return frame.form.getCenter().getWidth()*zoomScale()/candleWidth();
    }

    public int zoomScale(){
        return 1<<zoom;
    }

    private void fixFrom(){
        if (from<0) from = 0;
        from = from/zoomScale()*zoomScale();
        if (from>sheet.moments.size()-barsOnScreen())
            from = sheet.moments.size()-barsOnScreen();
    }

    public int candleWidth(){
        return 9;
    }

    public Sheet getSheet() {
        return sheet;
    }

    public void zoomPlus() {
        if (zoom>=9) return;
        zoom++;
        frame.form.setZoom(zoom);
        fixFrom();
        fireViewUpdated();
    }

    public void zoomMinus() {
        if (zoom<=0) return;
        zoom--;
        frame.form.setZoom(zoom);
    }

    public int getIndexAt(Point point) {
        return getIndex()+point.x*zoomScale()/candleWidth();
    }

    public void mouseMove(Point point) {
        startDrag = null;
        Arrays.stream(viewListeners.getListeners(VisualizatorMouseListener.class)).forEach(l -> l.visualizatorMouseMoved(point));
    }

    private Point startDrag;
    private int startDragIndex;
    public void mousePressed(Point point) {
        startDrag = point;
        startDragIndex = from;
    }

    public void mouseReleased(Point point) {
        startDrag = null;
    }
    public void mouseExited(Point point) {
        startDrag = null;
    }
    public void mouseDrag(Point point) {
        if (startDrag!=null){
            int dif = getIndexAt(point)-getIndexAt(startDrag);
            setIndex(startDragIndex-dif);
        }
    }

    public void mouseClicked(Point point) {
        Arrays.stream(viewListeners.getListeners(VisualizatorMouseListener.class)).forEach(l -> l.visualizatorMouseClicked(point));
    }

    public static void main(String[] args) {
        try {
            Sheet sheet = new Sheet();
            sheet.fromCache(500);
//            sheet.fromExchange();
            SheetUtils.FillDecisions(sheet);
            sheet.calcIndicators();
            new Visualizator(sheet);
        } catch (Exception e){
            e.printStackTrace();
        }

    }
}


package ru.efreet.trading.visual;

import java.awt.*;
import java.util.EventListener;

public interface VisualizatorMouseListener extends EventListener {
    void visualizatorMouseMoved(Point p);
    void visualizatorMouseClicked(Point p);
}

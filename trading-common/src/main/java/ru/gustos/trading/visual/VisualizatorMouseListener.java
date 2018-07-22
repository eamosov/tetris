package ru.gustos.trading.visual;

import java.awt.*;
import java.util.EventListener;

public interface VisualizatorMouseListener extends EventListener {
    default void visualizatorMouseMoved(Point p){}
    default void visualizatorMouseClicked(Point p, int button){}
    default void visualizatorMousePressed(Point p, int button){}
    default void visualizatorMouseReleased(Point p, int button){}
    default void visualizatorMouseDragged(Point p, int button){}
}

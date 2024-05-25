package com.mindolph.mindmap;

import javafx.scene.layout.Pane;

/**
 * @author mindolph.com@gmail.com
 */
public class MindMapContext {

    private double scale = 1.0f;

    private boolean debugMode = false;

    private Pane parentPane;

    public double getScale() {
        return scale;
    }

    public MindMapContext setScale(double scale) {
        this.scale = scale;
        return this;
    }

    public float safeScale(float value, float minimal) {
        float result = (float) (this.scale * value);
        return Float.compare(result, minimal) >= 0 ? result : minimal;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public Pane getParentPane() {
        return parentPane;
    }

    public void setParentPane(Pane parentPane) {
        this.parentPane = parentPane;
    }
}

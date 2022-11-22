package com.rebirth.qarobot.app.utils;

import javax.swing.ComboBoxModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataListener;
import java.awt.GraphicsDevice;

public class ComboBoxScreenModel implements ComboBoxModel<GraphicsDevice> {

    private final GraphicsDevice[] graphicsDevices;
    private GraphicsDevice selecterGraphicsDevice;
    protected EventListenerList listenerList = new EventListenerList();

    public ComboBoxScreenModel(GraphicsDevice[] graphicsDevice) {
        this.graphicsDevices = graphicsDevice;
    }

    @Override
    public void setSelectedItem(Object anItem) {
        selecterGraphicsDevice = (GraphicsDevice) anItem;
    }

    @Override
    public Object getSelectedItem() {
        return this.selecterGraphicsDevice;
    }

    @Override
    public int getSize() {
        return this.graphicsDevices.length;
    }

    @Override
    public GraphicsDevice getElementAt(int index) {
        return this.graphicsDevices[index];
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        listenerList.add(ListDataListener.class, l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        listenerList.remove(ListDataListener.class, l);
    }

}

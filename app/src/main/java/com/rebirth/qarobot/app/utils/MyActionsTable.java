package com.rebirth.qarobot.app.utils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class MyActionsTable extends JTable {

    @Override
    public TableCellRenderer getDefaultRenderer(Class<?> columnClass) {
        if (columnClass == Boolean.class) {
            return super.getDefaultRenderer(columnClass);
        }
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                ActionTableModel model = (ActionTableModel) table.getModel();
                Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                try {
                    Color color = model.getRowColour(row);
                    if (isSelected) {
                        component.setForeground(color);
                        component.setBackground(Color.DARK_GRAY);
                    } else {
                        component.setBackground(color);
                    }
                } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
                    if (isSelected) {
                        component.setForeground(Color.WHITE);
                        component.setBackground(Color.DARK_GRAY);
                    } else {
                        component.setBackground(row % 2 == 0 ? Color.LIGHT_GRAY : Color.WHITE);
                    }
                }
                return component;
            }
        };
    }


}

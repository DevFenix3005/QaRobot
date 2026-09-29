package com.rebirth.qarobot.app.utils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class MyActionsTable extends JTable {

    public MyActionsTable() {
        setFillsViewportHeight(true);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        applyTheme();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        applyTheme();
    }

    private void applyTheme() {
        setRowHeight(38);
        setShowVerticalLines(false);
        setShowHorizontalLines(false);
        setIntercellSpacing(new Dimension(0, 0));
        setBackground(themeColor("Table.background", getBackground()));
        setForeground(themeColor("Table.foreground", getForeground()));
        setSelectionBackground(themeColor("Table.selectionBackground", getSelectionBackground()));
        setSelectionForeground(themeColor("Table.selectionForeground", getSelectionForeground()));
        if (getTableHeader() != null) {
            getTableHeader().setPreferredSize(new Dimension(0, 42));
            getTableHeader().setReorderingAllowed(false);
        }
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component component = super.prepareRenderer(renderer, row, column);
        boolean selected = isCellSelected(row, column);
        Color background = getBackground();
        if (!selected && row % 2 != 0) {
            background = themeColor("Table.alternateRowColor",
                    blend(background, getForeground(), 0.025f));
        }
        component.setBackground(selected ? getSelectionBackground() : background);
        component.setForeground(selected ? getSelectionForeground() : getForeground());

        if (component instanceof JComponent cell) {
            cell.setOpaque(true);
            Color statusColor = getModel() instanceof ActionTableModel actions
                    ? actions.getRowColour(convertRowIndexToModel(row)) : null;
            Border padding = BorderFactory.createEmptyBorder(0, 10, 0, 10);
            if (column == 0 && statusColor != null) {
                padding = BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 3, 0, 0, statusColor), padding);
            } else if (column == 0) {
                padding = BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(0, 3, 0, 0), padding);
            }
            boolean focused = hasFocus() && row == getSelectionModel().getLeadSelectionIndex()
                    && column == getColumnModel().getSelectionModel().getLeadSelectionIndex();
            Border focus = focused ? UIManager.getBorder("Table.focusCellHighlightBorder") : null;
            cell.setBorder(focus == null ? padding : BorderFactory.createCompoundBorder(focus, padding));
        }
        return component;
    }

    private static Color themeColor(String key, Color fallback) {
        Color color = UIManager.getColor(key);
        return color != null ? color : fallback;
    }

    private static Color blend(Color background, Color foreground, float amount) {
        return new Color(
                Math.round(background.getRed() * (1 - amount) + foreground.getRed() * amount),
                Math.round(background.getGreen() * (1 - amount) + foreground.getGreen() * amount),
                Math.round(background.getBlue() * (1 - amount) + foreground.getBlue() * amount));
    }
}
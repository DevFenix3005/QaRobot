package com.rebirth.qarobot.app.utils;

import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionTypeWithTimeout;

import javax.swing.table.AbstractTableModel;
import java.awt.Color;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActionTableModel extends AbstractTableModel {

    private final transient List<BaseActionType> actionDtoList;
    private final List<Color> rowColours;

    public ActionTableModel(List<BaseActionType> actionDtoList) {
        this.actionDtoList = List.copyOf(actionDtoList);
        this.rowColours = new ArrayList<>(Collections.nCopies(actionDtoList.size(), null));
    }

    public List<BaseActionType> getActionDtoList() {
        return actionDtoList;
    }

    public void setRowColour(int row, Color colour) {
        if (row < 0 || row >= getRowCount()) return;
        rowColours.set(row, colour);
        fireTableRowsUpdated(row, row);
    }

    public Color getRowColour(int row) {
        return row >= 0 && row < rowColours.size() ? rowColours.get(row) : null;
    }

    public void clearRowColours() {
        Collections.fill(rowColours, null);
        if (getRowCount() > 0) {
            fireTableRowsUpdated(0, getRowCount() - 1);
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return rowIndex >= 0 && rowIndex < getRowCount() && columnIndex == 4;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= getRowCount()) return;
        BaseActionType action = actionDtoList.get(rowIndex);
        switch (columnIndex) {
            case 0 -> action.setId(value == null ? null : value.toString());
            case 1 -> action.setDesc(value == null ? null : value.toString());
            case 3 -> {
                if (!(value instanceof Number number)) return;
                action.setOrder(number.longValue());
            }
            case 4 -> {
                if (!(value instanceof Boolean skip)) return;
                action.setSkip(skip);
            }
            default -> {
                return;
            }
        }
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    @Override
    public int getRowCount() {
        return actionDtoList.size();
    }

    @Override
    public int getColumnCount() {
        return 6;
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case 0 -> "ID";
            case 1 -> "Descripción";
            case 2 -> "Espera";
            case 3 -> "Orden";
            case 4 -> "Omitir";
            case 5 -> "Acción";
            default -> "";
        };
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        BaseActionType action = actionDtoList.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> action.getId();
            case 1 -> action.getDesc();
            case 2 -> action instanceof BaseActionTypeWithTimeout timedAction
                    ? timedAction.getTimeout() : BigInteger.ZERO;
            case 3 -> action.getOrder();
            case 4 -> action.isSkip();
            case 5 -> action.getClass().getSimpleName().replaceFirst("(Action)?Type$", "");
            default -> null;
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 0, 1, 5 -> String.class;
            case 2 -> BigInteger.class;
            case 3 -> Long.class;
            case 4 -> Boolean.class;
            default -> Object.class;
        };
    }
}

package com.rebirth.qarobot.app.utils;

import com.google.common.collect.Lists;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionTypeWithTimeout;

import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ActionTableModel extends AbstractTableModel {

    private final transient List<BaseActionType> actionDtoList;
    private final List<Color> rowColours = Lists.newArrayList();

    public ActionTableModel(List<BaseActionType> actionDtoList) {
        this.actionDtoList = actionDtoList;
    }

    public void setRowColour(int row, Color c) {
        try {
            rowColours.set(row, c);
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
            rowColours.add(row, c);
        }
        fireTableRowsUpdated(row, row);
    }

    public Color getRowColour(int row) {
        return rowColours.get(row);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 4;
    }


    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        BaseActionType action = this.actionDtoList.get(rowIndex);
        if (columnIndex == 0) {
            action.setId(value.toString());
        } else if (columnIndex == 1) {
            action.setDesc(value.toString());
        } else if (columnIndex == 3) {
            action.setOrder((Long) value);
        } else if (columnIndex == 4) {
            action.setSkip((Boolean) value);
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
        switch (column) {
            case 0:
                return "ID";
            case 1:
                return "Descripcion";
            case 2:
                return "Tiempo de espera";
            case 3:
                return "Numero de ejecucion";
            case 4:
                return "Omitida";
            case 5:
                return "Accion";
            default:
                return "UNK";
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        BaseActionType action = this.actionDtoList.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return action.getId();
            case 1:
                return action.getDesc();
            case 2:
                try {
                    return ((BaseActionTypeWithTimeout) action).getTimeout();
                } catch (ClassCastException classCastException) {
                    return 0;
                }
            case 3:
                return action.getOrder();
            case 4:
                return action.isSkip();
            case 5:
                return action.getClass().getSimpleName();
            default:
                return "UNK";
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return this.getValueAt(0, columnIndex).getClass();
    }


}

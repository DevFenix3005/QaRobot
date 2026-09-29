package com.rebirth.qarobot.app.utils;

import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionTypeWithTimeout;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ActionTableModelTest {

    @Test
    void anEmptyModelExposesStableColumnTypes() {
        ActionTableModel model = new ActionTableModel(List.of());

        assertEquals(0, model.getRowCount());
        assertEquals(6, model.getColumnCount());
        assertEquals(String.class, model.getColumnClass(0));
        assertEquals(String.class, model.getColumnClass(1));
        assertEquals(BigInteger.class, model.getColumnClass(2));
        assertEquals(Long.class, model.getColumnClass(3));
        assertEquals(Boolean.class, model.getColumnClass(4));
        assertEquals(String.class, model.getColumnClass(5));
        assertNull(model.getRowColour(0));
    }

    @Test
    void statusUpdatesCanArriveOutOfOrderAndBeCleared() {
        ActionTableModel model = new ActionTableModel(List.of(
                new BaseActionType(), new BaseActionType(), new BaseActionType()));

        model.setRowColour(2, Color.GREEN);
        assertNull(model.getRowColour(0));
        assertNull(model.getRowColour(1));
        assertEquals(Color.GREEN, model.getRowColour(2));
        assertDoesNotThrow(() -> model.setRowColour(-1, Color.RED));
        assertDoesNotThrow(() -> model.setRowColour(3, Color.RED));

        model.clearRowColours();
        assertNull(model.getRowColour(2));
    }

    @Test
    void skipEditingUpdatesTheOriginalAction() {
        BaseActionType action = new BaseActionType();
        ActionTableModel model = new ActionTableModel(List.of(action));

        for (int column = 0; column < model.getColumnCount(); column++) {
            assertEquals(column == 4, model.isCellEditable(0, column));
        }
        model.setValueAt(true, 0, 4);

        assertTrue(action.isSkip());
        assertEquals(true, model.getValueAt(0, 4));
        assertSame(action, model.getActionDtoList().get(0));
    }

    @Test
    void timeoutValuesHaveTheSameTypeForEveryAction() {
        BaseActionTypeWithTimeout timedAction = new BaseActionTypeWithTimeout();
        timedAction.setTimeout(BigInteger.valueOf(250));
        ActionTableModel model = new ActionTableModel(List.of(new BaseActionType(), timedAction));

        assertEquals(BigInteger.ZERO, model.getValueAt(0, 2));
        assertEquals(BigInteger.valueOf(250), model.getValueAt(1, 2));
    }
}

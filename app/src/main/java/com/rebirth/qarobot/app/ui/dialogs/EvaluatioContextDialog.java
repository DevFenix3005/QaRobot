/*
 * Created by JFormDesigner on Thu Aug 26 13:50:37 CDT 2021
 */

package com.rebirth.qarobot.app.ui.dialogs;

import com.rebirth.qarobot.commons.models.dtos.QaRobotContext;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

/**
 * @author Roberto Cazarin
 */
public class EvaluatioContextDialog extends JDialog {
    public EvaluatioContextDialog(Window owner) {
        super(owner);
        initComponents();
    }

    public void setUpTableData(QaRobotContext qaRobotContext) {
        DefaultTableModel tableModel = (DefaultTableModel) contextEvalTable.getModel();
        tableModel.setRowCount(0);
        Map<String, Object> maps = qaRobotContext.getMapContainer();
        maps.forEach((key, value) -> {
            String[] data = {key, value.toString()};
            tableModel.addRow(data);
        });
        contextEvalTable.setModel(tableModel);
        tableModel.fireTableDataChanged();
    }


    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - Roberto Cazarin
        scrollPane1 = new JScrollPane();
        contextEvalTable = new JTable();

        //======== this ========
        setTitle("Contexto de la ejecucion actual");
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout(25, 25));

        //======== scrollPane1 ========
        {

            //---- contextEvalTable ----
            contextEvalTable.setModel(new DefaultTableModel(
                    new Object[][]{
                    },
                    new String[]{
                            "Variable", "Valor"
                    }
            ) {
                Class<?>[] columnTypes = new Class<?>[]{
                        String.class, String.class
                };
                boolean[] columnEditable = new boolean[]{
                        true, false
                };

                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    return columnTypes[columnIndex];
                }

                @Override
                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return columnEditable[columnIndex];
                }
            });
            scrollPane1.setViewportView(contextEvalTable);
        }
        contentPane.add(scrollPane1, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - Roberto Cazarin
    private JScrollPane scrollPane1;
    private JTable contextEvalTable;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}

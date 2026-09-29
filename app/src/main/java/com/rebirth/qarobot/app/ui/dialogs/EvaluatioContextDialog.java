package com.rebirth.qarobot.app.ui.dialogs;

import com.rebirth.qarobot.app.ui.UiStyles;
import com.rebirth.qarobot.commons.models.dtos.QaRobotContext;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Objects;

public class EvaluatioContextDialog extends JDialog {
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Variable", "Valor"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
        @Override public Class<?> getColumnClass(int column) { return String.class; }
    };
    private final JTable contextEvalTable = new JTable(model);
    private final JLabel count = UiStyles.muted("0 variables");

    public EvaluatioContextDialog(Window owner) {
        super(owner);
        setTitle("Contexto de ejecución · QaRobot");
        setMinimumSize(new Dimension(540, 360));
        JPanel body = UiStyles.card(new BorderLayout(0, 18), 24);
        JPanel header = UiStyles.transparent(new BorderLayout(0, 8));
        header.add(UiStyles.title("Contexto de ejecución", 21), BorderLayout.NORTH);
        header.add(UiStyles.muted("Consulta las variables disponibles en la prueba actual."), BorderLayout.CENTER);
        body.add(header, BorderLayout.NORTH);
        contextEvalTable.setName("contextTable");
        contextEvalTable.setRowHeight(36);
        contextEvalTable.setShowGrid(false);
        contextEvalTable.setFillsViewportHeight(true);
        contextEvalTable.setAutoCreateRowSorter(true);
        contextEvalTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        contextEvalTable.getColumnModel().getColumn(1).setPreferredWidth(420);
        JScrollPane scroll = new JScrollPane(contextEvalTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(new Dimension(600, 300));
        body.add(scroll, BorderLayout.CENTER);
        JPanel footer = UiStyles.transparent(new BorderLayout());
        footer.add(count, BorderLayout.WEST);
        JButton close = new JButton("Cerrar");
        close.addActionListener(e -> setVisible(false));
        footer.add(close, BorderLayout.EAST);
        body.add(footer, BorderLayout.SOUTH);
        setContentPane(body);
        UiStyles.dialogKeys(this, close);
    }

    public void setUpTableData(QaRobotContext context) {
        model.setRowCount(0);
        context.getMapContainer().forEach((key, value) -> model.addRow(new String[]{key, Objects.toString(value, "null")}));
        count.setText(model.getRowCount() + (model.getRowCount() == 1 ? " variable" : " variables"));
    }
}

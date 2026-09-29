package com.rebirth.qarobot.app.ui;

import javax.swing.*;
import java.awt.*;

/** Small shared building blocks for the hand-maintained Swing layouts. */
public final class UiStyles {
    private UiStyles() { }

    public static JPanel transparent(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    public static JPanel card(LayoutManager layout, int padding) {
        JPanel panel = new JPanel(layout) {
            @Override public void updateUI() {
                super.updateUI();
                Color color = UIManager.getColor("QaRobot.cardBackground");
                if (color != null) setBackground(color);
            }
        };
        panel.setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
        return panel;
    }

    public static JLabel title(String text, int size) {
        JLabel label = new JLabel(text);
        label.putClientProperty("FlatLaf.style", "font: bold " + size);
        return label;
    }

    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.putClientProperty("FlatLaf.style", "foreground: $QaRobot.mutedForeground");
        return label;
    }

    public static JPanel field(String caption, JComponent component) {
        JPanel field = transparent(new BorderLayout(0, 6));
        JLabel label = muted(caption);
        label.setLabelFor(component);
        field.add(label, BorderLayout.NORTH);
        field.add(component, BorderLayout.CENTER);
        return field;
    }

    public static void dialogKeys(JDialog dialog, JButton close) {
        dialog.getRootPane().setDefaultButton(close);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.setVisible(false),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    }
}

package com.rebirth.qarobot.app.ui.dialogs;

import com.rebirth.qarobot.app.ui.UiStyles;
import com.rebirth.qarobot.commons.models.dtos.dialogs.TitleIconAndMsgPojo;

import javax.swing.*;
import java.awt.*;

public class QaDialog extends JDialog {
    private final JLabel heading = UiStyles.title("Mensaje", 21);
    private final JLabel icon = new JLabel();
    private final JEditorPane content = new JEditorPane();

    public QaDialog(Window owner) {
        super(owner);
        setTitle("QaRobot");
        setMinimumSize(new Dimension(520, 340));
        JPanel body = UiStyles.card(new BorderLayout(0, 20), 24);
        JPanel header = UiStyles.transparent(new BorderLayout(12, 0));
        header.add(icon, BorderLayout.WEST);
        header.add(heading, BorderLayout.CENTER);
        body.add(header, BorderLayout.NORTH);
        content.setContentType("text/html");
        content.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        content.setEditable(false);
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(new Dimension(520, 240));
        body.add(scroll, BorderLayout.CENTER);
        JPanel footer = UiStyles.transparent(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        JButton close = new JButton("Entendido");
        close.addActionListener(e -> setVisible(false));
        footer.add(close);
        body.add(footer, BorderLayout.SOUTH);
        setContentPane(body);
        UiStyles.dialogKeys(this, close);
    }

    public void addContentText(TitleIconAndMsgPojo message) {
        setTitle(message.getTitle() + " · QaRobot");
        heading.setText(message.getTitle());
        content.setText(message.getMsg());
        content.setCaretPosition(0);
        icon.setIcon(message.getMyOwnIcos().getIcon());
    }

    public void hideDialog() { setVisible(false); }
}

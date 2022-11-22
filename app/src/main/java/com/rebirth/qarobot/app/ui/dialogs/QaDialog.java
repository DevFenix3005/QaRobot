/*
 * Created by JFormDesigner on Mon Aug 09 12:55:39 CDT 2021
 */

package com.rebirth.qarobot.app.ui.dialogs;

import lombok.extern.log4j.Log4j2;
import com.rebirth.qarobot.commons.models.dtos.dialogs.MyOwnIcos;
import com.rebirth.qarobot.commons.models.dtos.dialogs.TitleIconAndMsgPojo;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

/**
 * @author Roberto Cazarin
 */
@Log4j2
public class QaDialog extends JDialog {

    public QaDialog(Window owner) {
        super(owner);
        initComponents();
    }

    public void addContentText(TitleIconAndMsgPojo titleIconAndMsgPojo) {
        this.setTitle(titleIconAndMsgPojo.getTitle());
        this.editorPane1.setText(titleIconAndMsgPojo.getMsg());
        this.addIcon2IconLabel(titleIconAndMsgPojo.getMyOwnIcos());
    }

    private void addIcon2IconLabel(MyOwnIcos myOwnIcos) {
        ImageIcon icon = myOwnIcos.getIcon();
        label1.setIcon(icon);
    }

    public void hideDialog() {
        this.setVisible(false);
    }

    private void okButtonActionPerformed() {
        this.dispose();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - Roberto Cazarin
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        label1 = new JLabel();
        scrollPane1 = new JScrollPane();
        editorPane1 = new JEditorPane();
        buttonBar = new JPanel();
        okButton = new JButton();

        //======== this ========
        setTitle("Dialogo");
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new GridBagLayout());
                ((GridBagLayout) contentPanel.getLayout()).columnWidths = new int[]{0, 0, 0, 0};
                ((GridBagLayout) contentPanel.getLayout()).rowHeights = new int[]{0, 0};
                ((GridBagLayout) contentPanel.getLayout()).columnWeights = new double[]{0.0, 0.0, 0.0, 1.0E-4};
                ((GridBagLayout) contentPanel.getLayout()).rowWeights = new double[]{0.0, 1.0E-4};
                contentPanel.add(label1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                        new Insets(0, 0, 0, 5), 0, 0));

                //======== scrollPane1 ========
                {

                    //---- editorPane1 ----
                    editorPane1.setEditable(false);
                    editorPane1.setContentType("text/html");
                    editorPane1.setBackground(new Color(153, 153, 153));
                    editorPane1.setFont(new Font("Consolas", Font.PLAIN, 10));
                    editorPane1.setForeground(Color.black);
                    editorPane1.setBorder(new TitledBorder(new BevelBorder(BevelBorder.LOWERED), "Mensaje", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION, null, Color.black));
                    scrollPane1.setViewportView(editorPane1);
                }
                contentPanel.add(scrollPane1, new GridBagConstraints(1, 0, 1, 1, 2.0, 2.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 5), 0, 0));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout) buttonBar.getLayout()).columnWidths = new int[]{0, 80};
                ((GridBagLayout) buttonBar.getLayout()).columnWeights = new double[]{1.0, 0.0};

                //---- okButton ----
                okButton.setText("OK");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - Roberto Cazarin
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JLabel label1;
    private JScrollPane scrollPane1;
    private JEditorPane editorPane1;
    private JPanel buttonBar;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}

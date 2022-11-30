/*
 * Created by JFormDesigner on Mon Jul 12 13:22:11 CDT 2021
 */

package com.rebirth.qarobot.app.ui.mainview;

import dagger.Lazy;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.disposables.Disposable;
import com.rebirth.qarobot.app.ui.dialogs.EvaluatioContextDialog;
import com.rebirth.qarobot.app.ui.dialogs.QaDialog;
import com.rebirth.qarobot.app.utils.ActionTableModel;
import com.rebirth.qarobot.app.utils.ComboBoxScreenModel;
import com.rebirth.qarobot.app.utils.MyActionsTable;
import com.rebirth.qarobot.app.viewmodel.MainViewModel;
import com.rebirth.qarobot.commons.models.dtos.dialogs.MyOwnIcos;
import com.rebirth.qarobot.commons.models.dtos.dialogs.TitleIconAndMsgPojo;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionType;
import com.rebirth.qarobot.commons.models.dtos.tables.ActionColorAndExIfExits;
import com.rebirth.qarobot.commons.models.dtos.toggle.PauseOrResumeState;
import com.rebirth.qarobot.scraping.enums.Browser;
import com.rebirth.qarobot.commons.models.dtos.QaRobotContext;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * @author unknown
 */
@Singleton
public class MyMainView extends JPanel {


    @Inject
    public MyMainView(MainViewModel mainViewModel, ComboBoxScreenModel comboBoxScreenModel) {
        this.mainViewModel = mainViewModel;
        this.comboBoxScreenModel = comboBoxScreenModel;
        initComponents();
    }

    public void initEvents() {
        this.comboBox2.setModel(comboBoxScreenModel);

        this.buscarXmlButton.addMouseListener(this.mainViewModel.chooseXmlPathMouseAdapter);
        this.initButton.addMouseListener(this.mainViewModel.initProcessorMouseAdapter);
        this.reloadButton.addMouseListener(this.mainViewModel.reloadQaRobotMouseAdapter);

        this.spinner1.addChangeListener(this.mainViewModel::stateChanged);

        this.comboBox1.addItemListener(this.mainViewModel::changeComboItem);
        this.checkBox1.addItemListener(this.mainViewModel::checkAction);
        this.comboBox2.addItemListener(this.mainViewModel::changeComboItemGraphicsDevice);
        this.pauseOrResumenButton.addActionListener(this.mainViewModel::pauseOrResumenButtonListener);

        @NonNull Disposable textField1Onservable = this.mainViewModel.getXmlPath().subscribe(this.textField1::setText);
        @NonNull Disposable textField2Onservable = this.mainViewModel.getStatusBar().subscribe(this.textField2::setText);
        @NonNull Disposable table1Onservable = this.mainViewModel.getActionDtos().subscribe(value -> this.table1.setModel(new ActionTableModel(value)));
        @NonNull Disposable initButtonOnservable = this.mainViewModel.getStatusInitButton().subscribe(value -> this.initButton.setEnabled(value));
        @NonNull Disposable reloadButtonOnservable = this.mainViewModel.getStatusReloadButton().subscribe(value -> this.reloadButton.setEnabled(value));
        @NonNull Disposable buscarXmlButtonOnservable = this.mainViewModel.getSearchButton().subscribe(value -> this.buscarXmlButton.setEnabled(value));
        @NonNull Disposable myModalOnservable = this.mainViewModel.getSendInfo2MyDialog().subscribe(this::showInfoDialog);
        @NonNull Disposable hideModalOnservable = this.mainViewModel.getHideMyDialog().subscribe(this::hideMyModal);
        @NonNull Disposable chnageRowStatsuOnservable = this.mainViewModel.getInteraccionData2ChangeAdvanceInTable().subscribe(this::changeMyTableStatus);
        @NonNull Disposable changeTogglePauseResumenButtonDisposable = this.mainViewModel
                .getPauseOrResumenActionExecution()
                .filter(state -> state != PauseOrResumeState.NONE)
                .subscribe(this::changePauseOrResumenToogleButton);

        @NonNull Disposable pauseOrResumenButtonStatusDisposable = this.mainViewModel.getPauseOrResumenStatus().subscribe(value -> this.pauseOrResumenButton.setEnabled(value));
        @NonNull Disposable showMyEvalTableDisposable = this.mainViewModel.getShowEvalTable().subscribe(this::showMyEvalTable);

        disposables.add(textField1Onservable);
        disposables.add(textField2Onservable);
        disposables.add(table1Onservable);
        disposables.add(initButtonOnservable);
        disposables.add(reloadButtonOnservable);
        disposables.add(buscarXmlButtonOnservable);
        disposables.add(myModalOnservable);
        disposables.add(hideModalOnservable);
        disposables.add(chnageRowStatsuOnservable);
        disposables.add(changeTogglePauseResumenButtonDisposable);
        disposables.add(pauseOrResumenButtonStatusDisposable);
        disposables.add(showMyEvalTableDisposable);

        this.comboBox1.setSelectedItem(this.mainViewModel.getBrowser().getValue());
        this.spinner1.setValue(this.mainViewModel.getInteracion().getValue());

    }

    private void changePauseOrResumenToogleButton(PauseOrResumeState pauseOrResumeState) {
        this.pauseOrResumenButton.setText(pauseOrResumeState.getLiteralValue());
        this.pauseOrResumenButton.setSelected(pauseOrResumeState == PauseOrResumeState.RESUME);
    }

    private void changeMyTableStatus(ActionColorAndExIfExits actionColorAndExIfExits) {
        BaseActionType baseActionDto = actionColorAndExIfExits.getBaseActionType();
        Color color = actionColorAndExIfExits.getColor();
        this.setCurrentActionInTable(baseActionDto, color);
        this.mainViewModel.getStatusBar().onNext("Prueba: " + baseActionDto.getDesc());

        Throwable throwable = actionColorAndExIfExits.getThrowable();
        if (throwable != null) {
            this.mainViewModel.finishQaWithError(throwable);
        }
    }

    private void showInfoDialog(TitleIconAndMsgPojo titleIconAndMsgPojo) {

        Runnable runnable = () -> {
            QaDialog qaDialog = this.lazyQaDialog.get();
            Point point = titleIconAndMsgPojo.getPoint();
            Dimension dimension = titleIconAndMsgPojo.getDimension();

            qaDialog.addContentText(titleIconAndMsgPojo);

            if (point != null) {
                qaDialog.setLocation(point);
            } else {
                qaDialog.setLocationRelativeTo(MyMainView.this);
            }

            if (dimension != null) {
                qaDialog.setSize(dimension);
            } else {
                qaDialog.pack();
            }

            if (!qaDialog.isVisible()) {
                qaDialog.setVisible(true);
            }
        };

        if (titleIconAndMsgPojo.isRunInThread()) {
            SwingUtilities.invokeLater(runnable);
        } else {
            runnable.run();
        }
    }


    private void showMyEvalTable(QaRobotContext qaRobotContext) {
        SwingUtilities.invokeLater(() -> {
            EvaluatioContextDialog evaluationtable = lazyEvaluatioContextDialog.get();
            evaluationtable.setUpTableData(qaRobotContext);
            evaluationtable.pack();
            if (!evaluationtable.isVisible()) {
                evaluationtable.setVisible(true);
            }
        });
    }


    public void hideMyModal(boolean flag) {
        SwingUtilities.invokeLater(() -> {
            QaDialog lazyDialog = lazyQaDialog.get();
            lazyDialog.setVisible(false);
        });
    }


    public void setCurrentActionInTable(BaseActionType action, Color color) {
        ActionTableModel actionTableModel = (ActionTableModel) this.table1.getModel();
        List<BaseActionType> actionDtos = actionTableModel.getActionDtoList();
        int index = actionDtos.indexOf(action);
        if (index != -1) {
            actionTableModel.setRowColour(index, color);
        }
    }


    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - Roberto Cazarin
        ResourceBundle bundle = ResourceBundle.getBundle("strings");
        panel1 = new JPanel();
        label1 = new JLabel();
        textField1 = new JTextField();
        buscarXmlButton = new JButton();
        label2 = new JLabel();
        comboBox1 = new JComboBox<>();
        comboBox1.setModel(new DefaultComboBoxModel<Browser>(Browser.values()));
        label3 = new JLabel();
        spinner1 = new JSpinner();
        initButton = new JButton();
        reloadButton = new JButton();
        label4 = new JLabel();
        panel3 = new JPanel();
        checkBox1 = new JCheckBox();
        comboBox2 = new JComboBox<>();
        pauseOrResumenButton = new JToggleButton(MyOwnIcos.PAUSE_MDPI.getIcon());
        pauseOrResumenButton.setSelectedIcon(MyOwnIcos.PLAY_MDPI.getIcon());
        scrollPane1 = new JScrollPane();
        table1 = new MyActionsTable();
        panel2 = new JPanel();
        textField2 = new JTextField();

        //======== this ========
        setLayout(new BorderLayout());

        //======== panel1 ========
        {
            panel1.setBorder(new TitledBorder("Control"));
            panel1.setLayout(new GridBagLayout());
            ((GridBagLayout) panel1.getLayout()).columnWidths = new int[]{0, 0, 0, 0};
            ((GridBagLayout) panel1.getLayout()).rowHeights = new int[]{0, 0, 0, 0, 0};
            ((GridBagLayout) panel1.getLayout()).columnWeights = new double[]{0.0, 0.0, 0.0, 1.0E-4};
            ((GridBagLayout) panel1.getLayout()).rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 1.0E-4};

            //---- label1 ----
            label1.setText(bundle.getString("archivoxml_label"));
            panel1.add(label1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 5, 5), 0, 0));

            //---- textField1 ----
            textField1.setEditable(false);
            textField1.setDisabledTextColor(Color.white);
            panel1.add(textField1, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 5, 5), 0, 0));

            //---- buscarXmlButton ----
            buscarXmlButton.setText(bundle.getString("buscar_xml_button"));
            panel1.add(buscarXmlButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 5, 0), 0, 0));

            //---- label2 ----
            label2.setText(bundle.getString("navegador_label"));
            panel1.add(label2, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 5, 5), 0, 0));
            panel1.add(comboBox1, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 5, 5), 0, 0));

            //---- label3 ----
            label3.setText(bundle.getString("interaciones_label"));
            panel1.add(label3, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 5, 5), 0, 0));

            //---- spinner1 ----
            spinner1.setModel(new SpinnerNumberModel(1, null, null, 1));
            panel1.add(spinner1, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 5, 5), 0, 0));

            //---- initButton ----
            initButton.setText(bundle.getString("iniciar_button"));
            initButton.setEnabled(false);
            panel1.add(initButton, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 5, 0), 0, 0));

            //---- reloadButton ----
            reloadButton.setText(bundle.getString("reload_button"));
            reloadButton.setEnabled(false);
            panel1.add(reloadButton, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 5, 0), 0, 0));

            //---- label4 ----
            label4.setText(bundle.getString("opciones_label"));
            panel1.add(label4, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

            //======== panel3 ========
            {
                panel3.setLayout(new FlowLayout(FlowLayout.LEFT));

                //---- checkBox1 ----
                checkBox1.setText(bundle.getString("grabar_checkbutton"));
                checkBox1.setActionCommand(bundle.getString("grabar_checkbutton"));
                panel3.add(checkBox1);
                panel3.add(comboBox2);
            }
            panel1.add(panel3, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

            //---- pauseOrResumenButton ----
            pauseOrResumenButton.setText("Pausar");
            panel1.add(pauseOrResumenButton, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 0), 0, 0));
        }
        add(panel1, BorderLayout.NORTH);

        //======== scrollPane1 ========
        {
            scrollPane1.setViewportView(table1);
        }
        add(scrollPane1, BorderLayout.CENTER);

        //======== panel2 ========
        {
            panel2.setBorder(new BevelBorder(BevelBorder.LOWERED));
            panel2.setLayout(new BoxLayout(panel2, BoxLayout.X_AXIS));

            //---- textField2 ----
            textField2.setEditable(false);
            textField2.setText("ready!");
            panel2.add(textField2);
        }
        add(panel2, BorderLayout.SOUTH);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents

    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - Roberto Cazarin
    private JPanel panel1;
    private JLabel label1;
    private JTextField textField1;
    private JButton buscarXmlButton;
    private JLabel label2;
    private JComboBox<Browser> comboBox1;
    private JLabel label3;
    private JSpinner spinner1;
    private JButton initButton;
    private JButton reloadButton;
    private JLabel label4;
    private JPanel panel3;
    private JCheckBox checkBox1;
    private JComboBox<GraphicsDevice> comboBox2;
    private JToggleButton pauseOrResumenButton;
    private JScrollPane scrollPane1;
    private JTable table1;
    private JPanel panel2;
    private JTextField textField2;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
    private final MainViewModel mainViewModel;
    private final ComboBoxScreenModel comboBoxScreenModel;
    private Lazy<QaDialog> lazyQaDialog;
    private Lazy<EvaluatioContextDialog> lazyEvaluatioContextDialog;
    private final List<Disposable> disposables = new ArrayList<>();

    @Inject
    public void setLazyQaDialog(Lazy<QaDialog> lazyQaDialog) {
        this.lazyQaDialog = lazyQaDialog;
    }

    @Inject
    public void setEvaluatioContextDialog(Lazy<EvaluatioContextDialog> lazyEvaluatioContextDialog) {
        this.lazyEvaluatioContextDialog = lazyEvaluatioContextDialog;
    }

    public List<Disposable> getDisposables() {
        return disposables;
    }


}

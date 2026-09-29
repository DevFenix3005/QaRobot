package com.rebirth.qarobot.app.ui.mainview;

import com.rebirth.qarobot.app.ui.UiStyles;
import com.rebirth.qarobot.app.ui.dialogs.EvaluatioContextDialog;
import com.rebirth.qarobot.app.ui.dialogs.QaDialog;
import com.rebirth.qarobot.app.utils.ActionTableModel;
import com.rebirth.qarobot.app.utils.ComboBoxScreenModel;
import com.rebirth.qarobot.app.utils.MyActionsTable;
import com.rebirth.qarobot.app.viewmodel.MainViewModel;
import com.rebirth.qarobot.commons.models.dtos.QaRobotContext;
import com.rebirth.qarobot.commons.models.dtos.dialogs.TitleIconAndMsgPojo;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionType;
import com.rebirth.qarobot.commons.models.dtos.tables.ActionColorAndExIfExits;
import com.rebirth.qarobot.commons.models.dtos.toggle.PauseOrResumeState;
import com.rebirth.qarobot.scraping.enums.Browser;
import dagger.Lazy;
import io.reactivex.rxjava3.disposables.Disposable;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class MyMainView extends JPanel {
    private final MainViewModel mainViewModel;
    private final ComboBoxScreenModel comboBoxScreenModel;
    private final List<Disposable> disposables = new ArrayList<>();
    private Lazy<QaDialog> lazyQaDialog;
    private Lazy<EvaluatioContextDialog> lazyEvaluatioContextDialog;
    private boolean eventsInitialized;

    private final JTextField xmlPath = new JTextField();
    private final JButton browseButton = new JButton("Abrir XML…");
    private final JButton startButton = new JButton("Iniciar prueba");
    private final JButton reloadButton = new JButton("Recargar");
    private final JToggleButton pauseButton = new JToggleButton("Pausar");
    private final JComboBox<Browser> browser = new JComboBox<>(Browser.values());
    private final JSpinner iterations = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
    private final JCheckBox recording = new JCheckBox("Grabar ejecución");
    private final JComboBox<GraphicsDevice> screens = new JComboBox<>();
    private final JTable actions = new MyActionsTable();
    private final JLabel count = UiStyles.muted("0 acciones");
    private final JLabel status = UiStyles.muted("Abre un archivo XML para preparar tu prueba.");
    private final JLabel executionState = UiStyles.muted("SIN ESCENARIO");
    private final CardLayout tableLayout = new CardLayout();
    private final JPanel tableContent = UiStyles.transparent(tableLayout);
    private final JProgressBar progress = new JProgressBar();

    @Inject
    public MyMainView(MainViewModel mainViewModel, ComboBoxScreenModel comboBoxScreenModel) {
        this.mainViewModel = mainViewModel;
        this.comboBoxScreenModel = comboBoxScreenModel;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 20));
        setBorder(BorderFactory.createEmptyBorder(24, 28, 18, 28));
        setPreferredSize(new Dimension(1060, 840));
        JPanel top = UiStyles.transparent(new BorderLayout(0, 22));
        JPanel header = UiStyles.transparent(new BorderLayout());
        JPanel heading = UiStyles.transparent(new GridLayout(2, 1, 0, 5));
        heading.add(UiStyles.title("QaRobot", 28));
        heading.add(UiStyles.muted("Automatiza, ejecuta y revisa tus pruebas."));
        header.add(heading, BorderLayout.WEST);
        top.add(header, BorderLayout.NORTH);

        JPanel settings = UiStyles.card(new BorderLayout(0, 16), 20);
        JPanel fileRow = UiStyles.transparent(new BorderLayout(12, 0));
        xmlPath.setName("xmlPath");
        xmlPath.setEditable(false);
        xmlPath.putClientProperty("JTextField.placeholderText", "Selecciona un escenario de pruebas (.xml)");
        xmlPath.setToolTipText("Archivo XML del escenario seleccionado");
        browseButton.setName("browseXml");
        browseButton.setMnemonic('A');
        fileRow.add(xmlPath, BorderLayout.CENTER);
        fileRow.add(browseButton, BorderLayout.EAST);
        settings.add(UiStyles.field("Escenario de prueba", fileRow), BorderLayout.NORTH);

        JPanel options = UiStyles.transparent(new GridBagLayout());
        browser.setName("browserSelector");
        browser.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof Browser b) setText(switch (b) {
                    case CHROME -> "Google Chrome";
                    case FIREFOX -> "Mozilla Firefox";
                    case EDGE -> "Microsoft Edge";
                });
                return this;
            }
        });
        screens.setName("screenSelector");
        screens.setEnabled(false);
        screens.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof GraphicsDevice device) {
                    int number = 1;
                    for (int i = 0; i < comboBoxScreenModel.getSize(); i++) {
                        if (comboBoxScreenModel.getElementAt(i).equals(device)) number = i + 1;
                    }
                    DisplayMode mode = device.getDisplayMode();
                    setText("Pantalla " + number + " · " + mode.getWidth() + " × " + mode.getHeight());
                }
                return this;
            }
        });
        recording.setOpaque(false);
        JPanel recordOptions = UiStyles.transparent(new BorderLayout(0, 4));
        recordOptions.add(recording, BorderLayout.NORTH);
        recordOptions.add(screens, BorderLayout.CENTER);
        options.add(UiStyles.field("Navegador", browser), constraints(0, 0.36, 16));
        options.add(UiStyles.field("Iteraciones", iterations), constraints(1, 0.18, 16));
        options.add(recordOptions, constraints(2, 0.46, 0));
        settings.add(options, BorderLayout.CENTER);
        JPanel controls = UiStyles.transparent(new BorderLayout());
        JPanel statePanel = UiStyles.transparent(new FlowLayout(FlowLayout.LEFT, 0, 8));
        statePanel.add(executionState);
        controls.add(statePanel, BorderLayout.WEST);
        JPanel buttons = UiStyles.transparent(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        startButton.setName("startTest");
        startButton.putClientProperty("FlatLaf.style", "background: #2563eb; foreground: #ffffff; font: bold");
        startButton.setEnabled(false);
        reloadButton.setEnabled(false);
        pauseButton.setEnabled(false);
        buttons.add(reloadButton);
        buttons.add(pauseButton);
        buttons.add(startButton);
        controls.add(buttons, BorderLayout.EAST);
        settings.add(controls, BorderLayout.SOUTH);
        top.add(settings, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        JPanel scenario = UiStyles.card(new BorderLayout(0, 14), 20);
        JPanel tableHeading = UiStyles.transparent(new BorderLayout());
        tableHeading.add(UiStyles.title("Acciones del escenario", 17), BorderLayout.WEST);
        tableHeading.add(count, BorderLayout.EAST);
        scenario.add(tableHeading, BorderLayout.NORTH);
        actions.setName("actionsTable");
        actions.setModel(new ActionTableModel(List.of()));
        JScrollPane scroll = new JScrollPane(actions);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        tableContent.add(scroll, "table");
        JPanel empty = UiStyles.transparent(new GridBagLayout());
        JPanel emptyMessage = UiStyles.transparent(new GridLayout(3, 1, 0, 10));
        JLabel emptyTitle = UiStyles.title("Todo listo para tu próxima prueba", 19);
        emptyTitle.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel emptyText = UiStyles.muted("Abre un XML para ver sus acciones y configurar la ejecución.");
        emptyText.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel emptyHint = UiStyles.muted("Podrás omitir acciones antes de iniciar.");
        emptyHint.setHorizontalAlignment(SwingConstants.CENTER);
        emptyMessage.add(emptyTitle);
        emptyMessage.add(emptyText);
        emptyMessage.add(emptyHint);
        empty.add(emptyMessage);
        tableContent.add(empty, "empty");
        tableLayout.show(tableContent, "empty");
        scenario.add(tableContent, BorderLayout.CENTER);
        add(scenario, BorderLayout.CENTER);
        JPanel footer = UiStyles.transparent(new BorderLayout(16, 0));
        footer.add(status, BorderLayout.CENTER);
        progress.setPreferredSize(new Dimension(130, 5));
        progress.setVisible(false);
        footer.add(progress, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
    }

    private static GridBagConstraints constraints(int column, double weight, int right) {
        return new GridBagConstraints(column, 0, 1, 1, weight, 1, GridBagConstraints.SOUTH,
                GridBagConstraints.BOTH, new Insets(0, 0, 0, right), 0, 0);
    }

    public void initEvents() {
        if (eventsInitialized) return;
        eventsInitialized = true;
        screens.setModel(comboBoxScreenModel);
        browseButton.addActionListener(e -> mainViewModel.chooseXmlPath(this));
        startButton.addActionListener(e -> mainViewModel.startQaRobot());
        reloadButton.addActionListener(e -> mainViewModel.reloadQaRobot());
        iterations.addChangeListener(mainViewModel::stateChanged);
        browser.addItemListener(mainViewModel::changeComboItem);
        recording.addItemListener(mainViewModel::checkAction);
        screens.addItemListener(mainViewModel::changeComboItemGraphicsDevice);
        if (screens.getSelectedItem() instanceof GraphicsDevice device) mainViewModel.getSelectedScreen().onNext(device);
        pauseButton.addActionListener(mainViewModel::pauseOrResumenButtonListener);

        disposables.add(mainViewModel.getXmlPath().subscribe(value -> onEdt(() -> {
            xmlPath.setText(value);
            xmlPath.setToolTipText(mainViewModel.getSelectedFile().getValue() == null ? value
                    : mainViewModel.getSelectedFile().getValue().getAbsolutePath());
        })));
        disposables.add(mainViewModel.getStatusBar().subscribe(value -> onEdt(() -> {
            status.setText(value);
            status.setToolTipText(value);
        })));
        disposables.add(mainViewModel.getActionDtos().subscribe(value -> onEdt(() -> {
            actions.setModel(new ActionTableModel(value));
            int[] widths = {120, 320, 120, 100, 80, 150};
            for (int i = 0; i < widths.length; i++) actions.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            count.setText(value.size() + (value.size() == 1 ? " acción" : " acciones"));
            tableLayout.show(tableContent, value.isEmpty() ? "empty" : "table");
        })));
        disposables.add(mainViewModel.getStatusInitButton().subscribe(value -> onEdt(() -> {
            startButton.setEnabled(value);
            if (value) executionState.setText("LISTO PARA EJECUTAR");
        })));
        disposables.add(mainViewModel.getStatusReloadButton().subscribe(value -> onEdt(() -> reloadButton.setEnabled(value))));
        disposables.add(mainViewModel.getSearchButton().subscribe(value -> onEdt(() -> {
            browseButton.setEnabled(value);
            browser.setEnabled(value);
            iterations.setEnabled(value);
            recording.setEnabled(value);
            screens.setEnabled(value && recording.isSelected());
            actions.setEnabled(value);
        })));
        disposables.add(mainViewModel.getGrabar().subscribe(value -> onEdt(() -> screens.setEnabled(value && recording.isEnabled()))));
        disposables.add(mainViewModel.getSendInfo2MyDialog().subscribe(this::showInfoDialog));
        disposables.add(mainViewModel.getHideMyDialog().subscribe(this::hideMyModal));
        disposables.add(mainViewModel.getInteraccionData2ChangeAdvanceInTable().subscribe(this::changeMyTableStatus));
        disposables.add(mainViewModel.getPauseOrResumenActionExecution().subscribe(value -> onEdt(() -> {
            pauseButton.setSelected(value == PauseOrResumeState.RESUME);
            pauseButton.setText(value == PauseOrResumeState.RESUME ? "Reanudar" : "Pausar");
            if (pauseButton.isEnabled()) executionState.setText(value == PauseOrResumeState.RESUME ? "EN PAUSA" : "EN EJECUCIÓN");
        })));
        disposables.add(mainViewModel.getPauseOrResumenStatus().subscribe(value -> onEdt(() -> {
            pauseButton.setEnabled(value);
            if (!value) { pauseButton.setSelected(false); pauseButton.setText("Pausar"); }
            executionState.setText(value ? "EN EJECUCIÓN" : (actions.getRowCount() == 0 ? "SIN ESCENARIO" : "LISTO PARA EJECUTAR"));
            progress.setIndeterminate(value);
            progress.setVisible(value);
        })));
        disposables.add(mainViewModel.getShowEvalTable().subscribe(this::showMyEvalTable));
        browser.setSelectedItem(mainViewModel.getBrowser().getValue());
        iterations.setValue(mainViewModel.getInteracion().getValue());
    }

    private static void onEdt(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) action.run();
        else SwingUtilities.invokeLater(action);
    }

    private void changeMyTableStatus(ActionColorAndExIfExits update) {
        setCurrentActionInTable(update.getBaseActionType(), update.getColor());
        mainViewModel.getStatusBar().onNext("Acción: " + update.getBaseActionType().getDesc());
    }

    public void setCurrentActionInTable(BaseActionType action, Color color) {
        onEdt(() -> {
            ActionTableModel model = (ActionTableModel) actions.getModel();
            int row = model.getActionDtoList().indexOf(action);
            if (row >= 0) {
                model.setRowColour(row, color);
                actions.scrollRectToVisible(actions.getCellRect(row, 0, true));
            }
        });
    }

    private void showInfoDialog(TitleIconAndMsgPojo info) {
        onEdt(() -> {
            QaDialog dialog = lazyQaDialog.get();
            dialog.addContentText(info);
            dialog.pack();
            if (info.getDimension() != null) dialog.setSize(Math.max(520, info.getDimension().width), Math.max(340, info.getDimension().height));
            if (info.getPoint() != null) dialog.setLocation(info.getPoint());
            else dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        });
    }

    private void showMyEvalTable(QaRobotContext context) {
        onEdt(() -> {
            EvaluatioContextDialog dialog = lazyEvaluatioContextDialog.get();
            dialog.setUpTableData(context);
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        });
    }

    public void hideMyModal(boolean flag) { onEdt(() -> lazyQaDialog.get().setVisible(false)); }
    @Inject public void setLazyQaDialog(Lazy<QaDialog> dialog) { lazyQaDialog = dialog; }
    @Inject public void setEvaluatioContextDialog(Lazy<EvaluatioContextDialog> dialog) { lazyEvaluatioContextDialog = dialog; }
    public List<Disposable> getDisposables() { return disposables; }
}

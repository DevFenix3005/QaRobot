package com.rebirth.qarobot.app;

import com.jthemedetecor.OsThemeDetector;
import com.rebirth.qarobot.app.di.DaggerAppComponent;
import com.rebirth.qarobot.app.di.modules.AppModule;
import com.rebirth.qarobot.app.di.modules.PatternsModule;
import com.rebirth.qarobot.app.di.modules.XmlReaderModule;
import com.rebirth.qarobot.app.main.QAMaster;
import com.rebirth.qarobot.app.ui.MainFrame;
import com.rebirth.qarobot.app.ui.UiTheme;
import com.rebirth.qarobot.app.ui.dialogs.EvaluatioContextDialog;
import com.rebirth.qarobot.app.ui.dialogs.QaDialog;
import com.rebirth.qarobot.app.ui.mainview.MyMainView;
import com.rebirth.qarobot.app.utils.QaXmlReadService;
import com.rebirth.qarobot.app.viewmodel.MainViewModel;
import com.rebirth.qarobot.commons.di.enums.PatternEnum;
import com.rebirth.qarobot.commons.models.dtos.Configuracion;
import com.rebirth.qarobot.commons.models.dtos.QaRobotContext;
import com.rebirth.qarobot.commons.models.dtos.dialogs.MyOwnIcos;
import com.rebirth.qarobot.commons.models.dtos.dialogs.TitleIconAndMsgPojo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "qarobot.guiSmoke", matches = "true")
class GuiSmokeTest {
    @TempDir Path temp;

    private final List<Window> windows = new ArrayList<>();
    private final List<MyMainView> views = new ArrayList<>();
    private String previousWorkspace;

    @BeforeEach
    void initializeThemeInAnIsolatedWorkspace() throws Exception {
        previousWorkspace = System.getProperty("qarobot.workspace");
        System.setProperty("qarobot.workspace", temp.toString());
        SwingUtilities.invokeAndWait(UiTheme::initialize);
    }

    @AfterEach
    void closeWindowsAndRestoreWorkspace() throws Exception {
        try {
            SwingUtilities.invokeAndWait(() -> {
                UiTheme.shutdown();
                views.forEach(view -> view.getDisposables().forEach(disposable -> disposable.dispose()));
                windows.forEach(Window::dispose);
                UiTheme.apply(UiTheme.Theme.LIGHT);
            });
        } finally {
            if (previousWorkspace == null) System.clearProperty("qarobot.workspace");
            else System.setProperty("qarobot.workspace", previousWorkspace);
        }
    }

    @Test
    void realWindowUsesTheNativeThemeAndIgnoresLegacyPreferences() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JFrame window = DaggerAppComponent.factory().create(500).getMainFrame();
            windows.add(window);
            MyMainView view = requireComponent(window, MyMainView.class, null);
            views.add(view);
            window.pack();
            window.setVisible(true);
            view.initEvents();

            assertTrue(window.isShowing());
            assertTrue(window.getTitle().contains("QaRobot"));
            assertFalse(requireComponent(window, JButton.class, "startTest").isEnabled());
            assertEquals(0, requireComponent(window, JTable.class, "actionsTable").getRowCount());

            assertNull(findComponent(window, JComboBox.class, "themeSelector"));
            assertEquals(nativeTheme(), UiTheme.current());
            assertEquals(UIManager.getColor("Panel.background"), view.getBackground());
        });

        assertTrue(Files.isDirectory(temp.resolve("xmlenium")));
        Path legacyPreferences = temp.resolve("ui.properties");
        assertFalse(Files.exists(legacyPreferences));
        UiTheme.Theme opposite = nativeTheme() == UiTheme.Theme.DARK ? UiTheme.Theme.LIGHT : UiTheme.Theme.DARK;
        String oldPreference = "theme=" + opposite.name() + "\n";
        Files.writeString(legacyPreferences, oldPreference);

        SwingUtilities.invokeAndWait(() -> {
            UiTheme.shutdown();
            UiTheme.apply(opposite);
            UiTheme.initialize();
            assertEquals(nativeTheme(), UiTheme.current());
        });
        assertEquals(oldPreference, Files.readString(legacyPreferences));
    }

    @Test
    void anOfflineXmlPopulatesTheTableAndEnablesExecution() throws Exception {
        Configuracion configuration = new Configuracion();
        configuration.setDashboardsOutputs(temp.toFile());
        configuration.setXmlHome(temp.toFile());
        QaXmlReadService reader = new QaXmlReadService(XmlReaderModule.documentBuilderProvider(), configuration,
                AppModule.loremProvider(), new Random(1), Map.of(
                PatternEnum.STRING_PATTERN, PatternsModule.randomStringPattern(),
                PatternEnum.INTEGER_PATTERN, PatternsModule.randomIntegerPattern()));
        AtomicReference<MainViewModel> modelReference = new AtomicReference<>();
        QAMaster master = new QAMaster(reader,
                () -> { throw new AssertionError("Loading XML must not launch a browser"); },
                () -> { throw new AssertionError("Loading XML must not launch a recorder"); },
                modelReference::get);
        MainViewModel viewModel = new MainViewModel(master, configuration);
        modelReference.set(viewModel);
        Path example = Path.of("../examples/offline-smoke.xml").toAbsolutePath().normalize();

        try {
            SwingUtilities.invokeAndWait(() -> {
                MyMainView view = new MyMainView(viewModel,
                        AppModule.comboBoxScreenModelProvider(AppModule.graphicsDeviceProvider()));
                views.add(view);
                MainFrame window = new MainFrame(view);
                windows.add(window);
                QaDialog message = new QaDialog(window);
                EvaluatioContextDialog context = new EvaluatioContextDialog(window);
                view.setLazyQaDialog(() -> message);
                view.setEvaluatioContextDialog(() -> context);
                window.pack();
                window.setVisible(true);
                view.initEvents();

                viewModel.getSelectedFile().onNext(example.toFile());
                master.readQaRobot(example.toFile());

                assertNotNull(master.getQarobot());
                assertTrue(master.getQarobot().isValidXml(), () -> String.valueOf(master.getQarobot().getErrores()));
                JTable table = requireComponent(window, JTable.class, "actionsTable");
                assertEquals(6, table.getRowCount());
                assertTrue(table.isShowing());
                assertEquals(master.getQarobot().getActions().get(0).getId(), table.getValueAt(0, 0));
                assertTrue(requireComponent(window, JButton.class, "startTest").isEnabled());
                assertEquals(example.getFileName().toString(),
                        requireComponent(window, JTextField.class, "xmlPath").getText());

                table.setValueAt(true, 0, 4);
                assertTrue(master.getQarobot().getActions().get(0).isSkip());
            });
        } finally {
            if (master.getQarobot() != null && master.getQarobot().getXmlTempFile() != null) {
                Files.deleteIfExists(master.getQarobot().getXmlTempFile().toPath());
            }
        }
    }

    @Test
    void dialogsDisplayMessagesAndNullableContextInBothThemes() throws Exception {
        QaRobotContext context = new QaRobotContext(Map.of(
                PatternEnum.NUMBER_PATTERN, PatternsModule.numberPattern(),
                PatternEnum.MONEY_PATTERN, PatternsModule.moneyPattern()), new DecimalFormat());
        context.getMapContainer().put("nombre", "QaRobot");
        context.getMapContainer().put("intentos", 3);
        context.getMapContainer().put("pendiente", null);

        try {
            SwingUtilities.invokeAndWait(() -> {
                UiTheme.shutdown();
                JFrame owner = new JFrame("Dialog smoke test");
                windows.add(owner);
                QaDialog messageDialog = new QaDialog(owner);
                EvaluatioContextDialog contextDialog = new EvaluatioContextDialog(owner);
                messageDialog.addContentText(TitleIconAndMsgPojo.create("Verificación completa",
                        "<html><body><p>El escenario contiene seis acciones.</p></body></html>",
                        MyOwnIcos.INFO_MDPI));
                contextDialog.setUpTableData(context);

                Color previousMessageBackground = null;
                Color previousContextBackground = null;
                for (UiTheme.Theme theme : UiTheme.Theme.values()) {
                    UiTheme.apply(theme);
                    messageDialog.pack();
                    messageDialog.setVisible(true);
                    assertTrue(messageDialog.isShowing());
                    assertTrue(messageDialog.getTitle().contains("Verificación completa"));
                    JEditorPane text = requireComponent(messageDialog, JEditorPane.class, null);
                    String content = assertDoesNotThrow(
                            () -> text.getDocument().getText(0, text.getDocument().getLength()));
                    assertTrue(content.contains("El escenario contiene seis acciones."));
                    if (previousMessageBackground != null) {
                        assertNotEquals(previousMessageBackground, text.getBackground());
                    }
                    previousMessageBackground = text.getBackground();
                    messageDialog.getRootPane().getDefaultButton().doClick();
                    assertFalse(messageDialog.isVisible());

                    contextDialog.pack();
                    contextDialog.setVisible(true);
                    assertTrue(contextDialog.isShowing());
                    JTable table = requireComponent(contextDialog, JTable.class, "contextTable");
                    assertEquals(3, table.getRowCount());
                    assertEquals("3", valueFor(table, "intentos"));
                    assertEquals("QaRobot", valueFor(table, "nombre"));
                    assertEquals("null", valueFor(table, "pendiente"));
                    assertFalse(table.isCellEditable(0, 1));
                    if (previousContextBackground != null) {
                        assertNotEquals(previousContextBackground, table.getBackground());
                    }
                    previousContextBackground = table.getBackground();
                    contextDialog.getRootPane().getDefaultButton().doClick();
                    assertFalse(contextDialog.isVisible());
                }
            });
        } finally {
            context.shuwdownExecutor();
        }
    }

    private static UiTheme.Theme nativeTheme() {
        return OsThemeDetector.getDetector().isDark() ? UiTheme.Theme.DARK : UiTheme.Theme.LIGHT;
    }

    private static Object valueFor(JTable table, String key) {
        for (int row = 0; row < table.getRowCount(); row++) {
            if (key.equals(table.getValueAt(row, 0))) return table.getValueAt(row, 1);
        }
        fail("Missing context variable: " + key);
        return null;
    }

    private static <T extends Component> T requireComponent(Container root, Class<T> type, String name) {
        T component = findComponent(root, type, name);
        assertNotNull(component, () -> "Missing component: " + type.getSimpleName() + " " + name);
        return component;
    }

    private static <T extends Component> T findComponent(Component current, Class<T> type, String name) {
        if (type.isInstance(current) && (name == null || name.equals(current.getName()))) {
            return type.cast(current);
        }
        if (current instanceof Container container) {
            for (Component child : container.getComponents()) {
                T found = findComponent(child, type, name);
                if (found != null) return found;
            }
        }
        return null;
    }
}

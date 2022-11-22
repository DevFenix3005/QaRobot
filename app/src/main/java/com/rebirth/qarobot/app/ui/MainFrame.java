package com.rebirth.qarobot.app.ui;

import io.reactivex.rxjava3.disposables.Disposable;
import lombok.extern.log4j.Log4j2;
import com.rebirth.qarobot.app.ui.mainview.MyMainView;
import com.rebirth.qarobot.commons.models.dtos.dialogs.MyOwnIcos;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ResourceBundle;

@Singleton
@Log4j2
public class MainFrame extends JFrame {

    private final MyMainView myMainView;

    private static String generateTitle() {
        ResourceBundle bundle = ResourceBundle.getBundle("strings");
        String appName = bundle.getString("app_name");
        String appVerion = bundle.getString("app_verion");
        return appName + " [" + appVerion + "] ";
    }

    @Inject
    public MainFrame(JPanel jPanel) throws HeadlessException {
        super(generateTitle());
        this.myMainView = (MyMainView) jPanel;
        getContentPane().add(myMainView);
        setIcon();
        setWindowListeners();
    }

    private void setIcon() {
        try {
            setIconImage(ImageIO.read(MyOwnIcos.ROBOT_XXHDPI.getImageUrl()));
        } catch (IOException ioException) {
            log.error("MainFrame::setIcon -> IOException", ioException);
        }
    }

    private void setWindowListeners() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                for (Disposable disposable : myMainView.getDisposables()) {
                    disposable.dispose();
                }
            }

            @Override
            public void windowOpened(WindowEvent e) {
                super.windowOpened(e);
                myMainView.initEvents();
            }
        });
    }
}

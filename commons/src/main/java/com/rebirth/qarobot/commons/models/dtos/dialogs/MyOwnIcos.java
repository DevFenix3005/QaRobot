package com.rebirth.qarobot.commons.models.dtos.dialogs;

import com.google.common.io.Resources;

import javax.swing.*;
import java.net.URL;

public enum MyOwnIcos {

    PLAY_MDPI("ic_play.png", "Icono Play", 0),
    PAUSE_MDPI("ic_pausar.png", "Icono Pausar", 0),
    ROBOT_MDPI(MyOwnIcos.IC_ROBOT, MyOwnIcos.LOGO_IMAGE, 0),
    ERROR_MDPI(MyOwnIcos.IC_ERROR, MyOwnIcos.ERROR_IMAGE, 0),
    INFO_MDPI(MyOwnIcos.IC_INFO, MyOwnIcos.INFO_IMAGE, 0),
    WARNING_MDPI(MyOwnIcos.IC_WARING, MyOwnIcos.WARNING_IMAGE, 0),

    ROBOT_HDPI(MyOwnIcos.IC_ROBOT, MyOwnIcos.LOGO_IMAGE, 1),
    ERROR_HDPI(MyOwnIcos.IC_ERROR, MyOwnIcos.ERROR_IMAGE, 1),
    INFO_HDPI(MyOwnIcos.IC_INFO, MyOwnIcos.INFO_IMAGE, 1),
    WARNING_HDPI(MyOwnIcos.IC_WARING, MyOwnIcos.WARNING_IMAGE, 1),

    ROBOT_XHDPI(MyOwnIcos.IC_ROBOT, MyOwnIcos.LOGO_IMAGE, 2),
    ERROR_XHDPI(MyOwnIcos.IC_ERROR, MyOwnIcos.ERROR_IMAGE, 2),
    INFO_XHDPI(MyOwnIcos.IC_INFO, MyOwnIcos.INFO_IMAGE, 2),
    WARNING_XHDPI(MyOwnIcos.IC_WARING, MyOwnIcos.WARNING_IMAGE, 2),

    ROBOT_XXHDPI(MyOwnIcos.IC_ROBOT, MyOwnIcos.LOGO_IMAGE, 3),
    ERROR_XXHDPI(MyOwnIcos.IC_ERROR, MyOwnIcos.ERROR_IMAGE, 3),
    INFO_XXHDPI(MyOwnIcos.IC_INFO, MyOwnIcos.INFO_IMAGE, 3),
    WARNING_XXHDPI(MyOwnIcos.IC_WARING, MyOwnIcos.WARNING_IMAGE, 3),

    ROBOT_XXXHDPI(MyOwnIcos.IC_ROBOT, MyOwnIcos.LOGO_IMAGE, 4),
    ERROR_XXXHDPI(MyOwnIcos.IC_ERROR, MyOwnIcos.ERROR_IMAGE, 4),
    INFO_XXXHDPI(MyOwnIcos.IC_INFO, MyOwnIcos.INFO_IMAGE, 4),
    WARNING_XXXHDPI(MyOwnIcos.IC_WARING, MyOwnIcos.WARNING_IMAGE, 4),

    ROBOT_XXXXHDPI(MyOwnIcos.IC_ROBOT, MyOwnIcos.LOGO_IMAGE, 5),
    ERROR_XXXXHDPI(MyOwnIcos.IC_ERROR, MyOwnIcos.ERROR_IMAGE, 5),
    INFO_XXXXHDPI(MyOwnIcos.IC_INFO, MyOwnIcos.INFO_IMAGE, 5),
    WARNING_XXXXHDPI(MyOwnIcos.IC_WARING, MyOwnIcos.WARNING_IMAGE, 5);

    private static final String IC_ROBOT = "ic_robot.png";
    private static final String LOGO_IMAGE = "Logo Image";

    private static final String IC_ERROR = "ic_error.png";
    private static final String ERROR_IMAGE = "Error icon";

    private static final String IC_INFO = "ic_info.png";
    private static final String INFO_IMAGE = "Info icon";

    private static final String IC_WARING = "ic_warning.png";
    private static final String WARNING_IMAGE = "Warning icon";
    private final String iconPath;
    private final String desc;
    private final int size;


    MyOwnIcos(String iconPath, String desc, int size) {
        this.iconPath = iconPath;
        this.desc = desc;
        this.size = size;
    }

    public String getIconPath() {
        String resolutionFolder = "com/rebirth/qarobot/app/ui/dialogs/icons/mipmap-";
        switch (size) {
            case 0 -> resolutionFolder += "mdpi";
            case 1 -> resolutionFolder += "hdpi";
            case 2 -> resolutionFolder += "xhdpi";
            case 3 -> resolutionFolder += "xxhdpi";
            case 4 -> resolutionFolder += "xxxhdpi";
            default -> resolutionFolder += "highres";
        }

        return resolutionFolder + "/" + iconPath;
    }

    public URL getImageUrl() {
        return Resources.getResource(getIconPath());
    }

    public ImageIcon getIcon() {
        return new ImageIcon(getImageUrl(), desc);
    }

}

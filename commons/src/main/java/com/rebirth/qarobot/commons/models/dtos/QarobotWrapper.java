package com.rebirth.qarobot.commons.models.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.rebirth.qarobot.commons.models.dtos.qarobot.BaseActionType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.ConfigurationType;
import com.rebirth.qarobot.commons.models.dtos.qarobot.Qarobot;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@NoArgsConstructor
public class QarobotWrapper {

    private Qarobot qarobot;
    private boolean validXml;
    private List<String> errores;

    private File xmlFile;
    private File dashboardExitFile;
    private File xmlTempFile;
    private File dashboardsFolder;

    public String getFileName() {
        return this.xmlFile.getName();
    }

    public File getDashboardExitFile() {
        if (Objects.isNull(dashboardExitFile)) {
            String timesufix = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace(':', '_');
            String foldername = getFileName() + "_" + timesufix;
            this.dashboardExitFile = new File(dashboardsFolder, foldername);
        }
        return this.dashboardExitFile;
    }


    public ConfigurationType getConfiguration() {
        return qarobot.getConfiguration();
    }

    public void setConfiguration(ConfigurationType value) {
        qarobot.setConfiguration(value);
    }

    public List<BaseActionType> getActions() {
        return qarobot.getOpenOrIncludeOrCycle();
    }


    public void addError(String error) {
        if (Objects.isNull(errores)) {
            errores = new ArrayList<>();
        }
        errores.add(error);
    }

}

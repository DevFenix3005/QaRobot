package com.rebirth.qarobot.commons.models.dtos;

import com.google.common.base.MoreObjects;
import lombok.Data;

import java.io.File;
import java.io.Serializable;

@Data
public class Configuracion implements Serializable {
    private File scriptsHome;
    private File xmlHome;
    private File dashboardsOutputs;

    private File webdriverHome;
    private File dashboardtemplateHome;

    private long baseTimeout;

    public long tinyTimeout() {
        return baseTimeout;
    }

    public long timeout() {
        return baseTimeout * 5;
    }

    public long longTimeout() {
        return baseTimeout * 10;
    }

    public long xtraLongYimeout() {
        return baseTimeout * 20;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("webdriverHome", webdriverHome)
                .add("scriptsHome", scriptsHome)
                .add("xmlHome", xmlHome)
                .add("dashboardsOutputs", dashboardsOutputs)
                .add("baseTimeout", baseTimeout)
                .add("dashboardtemplateHome", dashboardtemplateHome)
                .toString();
    }
}


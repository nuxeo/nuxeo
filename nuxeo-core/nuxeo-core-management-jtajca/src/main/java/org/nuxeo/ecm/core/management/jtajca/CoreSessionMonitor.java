package org.nuxeo.ecm.core.management.jtajca;

import javax.management.MXBean;

@MXBean
public interface CoreSessionMonitor extends Monitor {

    public static String NAME = Defaults.instance.name(CoreSessionMonitor.class);

    int getCount();

    String[] getInfos();
}

package org.nuxeo.ecm.core.management.jtajca;

import javax.management.MXBean;

@MXBean
public interface CoreSessionMonitor {

    public static String NAME = Defaults.instance.name(CoreSessionMonitor.class);

    int getCount();

    String[] getInfos();
}

package org.nuxeo.ecm.core.management.works;

import javax.management.MXBean;

@MXBean
public interface WorksQueueMonitoringMBean {

    int getScheduledCount();

    int getRunningCount();

    int getCompletedCount();

    String[] getScheduledWorks();

    String[] getRunningWorks();

}
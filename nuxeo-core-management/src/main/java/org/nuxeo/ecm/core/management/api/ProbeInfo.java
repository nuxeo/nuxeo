package org.nuxeo.ecm.core.management.api;

import java.util.Date;

import org.nuxeo.ecm.core.management.probes.ProbeDescriptor;


public interface ProbeInfo {

    long getFailedCount();

    long getLastDuration();

    Date getLastFailedDate();

    Date getLastRunnedDate();

    Date getLastSucceedDate();

    long getRunnedCount();

    long getSucceedCount();

    boolean isEnabled();

    boolean isInError();

    ProbeStatus getStatus();

    String getShortcutName();

    String getQualifiedName();

    ProbeStatus getLastFailureStatus();

    ProbeDescriptor getDescriptor();

}
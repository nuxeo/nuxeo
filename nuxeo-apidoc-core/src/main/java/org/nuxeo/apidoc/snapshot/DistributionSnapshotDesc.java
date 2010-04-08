package org.nuxeo.apidoc.snapshot;

import java.util.Date;

public interface DistributionSnapshotDesc {

    String getVersion();

    String getName();

    Date getCreationDate();

    boolean isLive();
}

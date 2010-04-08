package org.nuxeo.apidoc.snapshot;

import java.util.Date;

public class DistributionSnapshotDescImpl implements DistributionSnapshotDesc {

    protected Date created;
    protected String name;
    protected String version;
    protected boolean live;

    public Date getCreationDate() {
        return created;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public boolean isLive() {
        return live;
    }

}

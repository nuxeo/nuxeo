package org.nuxeo.ecm.webapp.versioning;

import java.io.Serializable;

/**
 * Simple class that will be outjected into seam context to avoid multiple calls
 *
 * @author tiry
 */
public class VersionInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String versionLabel;

    protected boolean available;

    public VersionInfo(String label, boolean avalaible) {
        available = avalaible;
        versionLabel = label;
    }

    public String getVersionLabel() {
        return versionLabel;
    }

    public void setVersionLabel(String versionLabel) {
        this.versionLabel = versionLabel;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

}

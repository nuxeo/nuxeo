/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.api.impl;

import java.util.Calendar;

import org.nuxeo.targetplatforms.api.TargetInfo;


/**
 * @since 2.18
 */
public class TargetInfoImpl implements TargetInfo {

    private static final long serialVersionUID = 1L;

    protected String id;

    protected String name;

    protected String version;

    protected String refVersion;

    protected String label;

    protected String description;

    protected String status;

    protected boolean enabled = true;

    protected boolean restricted = false;

    protected Calendar releaseDate;

    protected Calendar endOfAvailability;

    protected String downloadLink;

    protected boolean deprecated = false;

    // needed by GWT serialization
    protected TargetInfoImpl() {
    }

    public TargetInfoImpl(String id) {
        this.id = id;
    }

    public TargetInfoImpl(String id, String name, String version,
            String refVersion, String label) {
        this(id);
        this.name = name;
        this.version = version;
        this.refVersion = refVersion;
        this.label = label;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String getRefVersion() {
        if (refVersion == null) {
            return version;
        }
        return refVersion;
    }

    public void setRefVersion(String refVersion) {
        this.refVersion = refVersion;
    }

    @Override
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRestricted() {
        return restricted;
    }

    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }

    public Calendar getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Calendar releaseDate) {
        this.releaseDate = releaseDate;
    }

    public Calendar getEndOfAvailability() {
        return endOfAvailability;
    }

    public void setEndOfAvailability(Calendar endOfAvailability) {
        this.endOfAvailability = endOfAvailability;
    }

    public String getDownloadLink() {
        return downloadLink;
    }

    public void setDownloadLink(String downloadLink) {
        this.downloadLink = downloadLink;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();

        buf.append(getClass().getSimpleName());
        buf.append(" {");
        buf.append(" id=");
        buf.append(id);
        buf.append(", name=");
        buf.append(name);
        buf.append(", version=");
        buf.append(version);
        buf.append(", refVersion=");
        buf.append(refVersion);
        buf.append(", label=");
        buf.append(label);
        buf.append('}');

        return buf.toString();
    }

}
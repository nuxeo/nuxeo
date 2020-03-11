/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.api.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.nuxeo.targetplatforms.api.TargetInfo;

/**
 * @since 5.7.1
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

    protected boolean fastTrack = false;

    protected boolean trial = false;

    protected boolean isDefault = false;

    protected Date releaseDate;

    protected Date endOfAvailability;

    protected String downloadLink;

    protected boolean deprecated = false;

    protected boolean overridden = false;

    protected List<String> types;

    // needed by GWT serialization
    protected TargetInfoImpl() {
    }

    public TargetInfoImpl(String id) {
        this.id = id;
    }

    public TargetInfoImpl(String id, String name, String version, String refVersion, String label) {
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

    @Override
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

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isRestricted() {
        return restricted;
    }

    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }

    @Override
    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    @Override
    public Date getEndOfAvailability() {
        return endOfAvailability;
    }

    public void setEndOfAvailability(Date endOfAvailability) {
        this.endOfAvailability = endOfAvailability;
    }

    @Override
    public String getDownloadLink() {
        return downloadLink;
    }

    public void setDownloadLink(String downloadLink) {
        this.downloadLink = downloadLink;
    }

    @Override
    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    @Override
    public boolean isTrial() {
        return trial;
    }

    public void setTrial(boolean trial) {
        this.trial = trial;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    @Override
    public boolean isFastTrack() {
        return fastTrack;
    }

    public void setFastTrack(boolean fastTrack) {
        this.fastTrack = fastTrack;
    }

    @Override
    public boolean isOverridden() {
        return overridden;
    }

    public void setOverridden(boolean overridden) {
        this.overridden = overridden;
    }

    @Override
    public List<String> getTypes() {
        if (types == null) {
            return Collections.emptyList();
        }
        return types;
    }

    public void setTypes(List<String> types) {
        if (types == null) {
            this.types = null;
        } else {
            this.types = new ArrayList<>(types);
        }
    }

    @Override
    public boolean matchesType(String type) {
        if (types == null) {
            return false;
        }
        return types.contains(type);
    }

    // Class#getSimpleName not supported by GWT
    protected String getSimpleName() {
        return getClass().getName().substring(getClass().getName().lastIndexOf('.') + 1);
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();

        buf.append(getSimpleName());
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

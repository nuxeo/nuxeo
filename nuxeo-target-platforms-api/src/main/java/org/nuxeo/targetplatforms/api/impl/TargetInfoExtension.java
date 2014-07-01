/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.api.impl;

import java.util.Date;
import java.util.List;

import org.nuxeo.targetplatforms.api.TargetInfo;

/**
 * {@link TargetInfo} implementation relying on an original implementation,
 * useful for override when adding additional metadata.
 *
 * @since 5.7.1
 */
public class TargetInfoExtension implements TargetInfo {

    private static final long serialVersionUID = 1L;

    protected TargetInfo origInfo;

    // needed by GWT serialization
    protected TargetInfoExtension() {
    }

    public TargetInfoExtension(TargetInfo orig) {
        this.origInfo = orig;
    }

    @Override
    public String getId() {
        return origInfo.getId();
    }

    @Override
    public String getName() {
        return origInfo.getName();
    }

    @Override
    public String getVersion() {
        return origInfo.getVersion();
    }

    @Override
    public String getRefVersion() {
        return origInfo.getRefVersion();
    }

    @Override
    public String getStatus() {
        return origInfo.getStatus();
    }

    public String getLabel() {
        return origInfo.getLabel();
    }

    @Override
    public String getDescription() {
        return origInfo.getDescription();
    }

    public boolean isEnabled() {
        return origInfo.isEnabled();
    }

    public boolean isRestricted() {
        return origInfo.isRestricted();
    }

    public Date getReleaseDate() {
        return origInfo.getReleaseDate();
    }

    public Date getEndOfAvailability() {
        return origInfo.getEndOfAvailability();
    }

    public String getDownloadLink() {
        return origInfo.getDownloadLink();
    }

    public boolean isDeprecated() {
        return origInfo.isDeprecated();
    }

    public boolean isTrial() {
        return origInfo.isTrial();
    }

    public boolean isDefault() {
        return origInfo.isDefault();
    }

    @Override
    public boolean isFastTrack() {
        return origInfo.isFastTrack();
    }

    public boolean isOverridden() {
        return origInfo.isOverridden();
    }

    @Override
    public List<String> getTypes() {
        return origInfo.getTypes();
    }

    @Override
    public boolean matchesType(String type) {
        return origInfo.matchesType(type);
    }

    // Class#getSimpleName not supported by GWT
    protected String getSimpleName() {
        return getClass().getName().substring(
                getClass().getName().lastIndexOf('.') + 1);
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();

        buf.append(getSimpleName());
        buf.append(" {");
        buf.append(" id=");
        buf.append(getId());
        buf.append(", name=");
        buf.append(getName());
        buf.append(", version=");
        buf.append(getVersion());
        buf.append(", refVersion=");
        buf.append(getRefVersion());
        buf.append(", label=");
        buf.append(getLabel());
        buf.append('}');

        return buf.toString();
    }
}

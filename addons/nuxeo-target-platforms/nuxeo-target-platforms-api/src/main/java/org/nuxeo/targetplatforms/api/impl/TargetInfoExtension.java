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

import java.util.Date;
import java.util.List;

import org.nuxeo.targetplatforms.api.TargetInfo;

/**
 * {@link TargetInfo} implementation relying on an original implementation, useful for override when adding additional
 * metadata.
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
        origInfo = orig;
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

    @Override
    public String getLabel() {
        return origInfo.getLabel();
    }

    @Override
    public String getDescription() {
        return origInfo.getDescription();
    }

    @Override
    public boolean isEnabled() {
        return origInfo.isEnabled();
    }

    @Override
    public boolean isRestricted() {
        return origInfo.isRestricted();
    }

    @Override
    public Date getReleaseDate() {
        return origInfo.getReleaseDate();
    }

    @Override
    public Date getEndOfAvailability() {
        return origInfo.getEndOfAvailability();
    }

    @Override
    public String getDownloadLink() {
        return origInfo.getDownloadLink();
    }

    @Override
    public boolean isDeprecated() {
        return origInfo.isDeprecated();
    }

    @Override
    public boolean isTrial() {
        return origInfo.isTrial();
    }

    @Override
    public boolean isDefault() {
        return origInfo.isDefault();
    }

    @Override
    public boolean isFastTrack() {
        return origInfo.isFastTrack();
    }

    @Override
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
        return getClass().getName().substring(getClass().getName().lastIndexOf('.') + 1);
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

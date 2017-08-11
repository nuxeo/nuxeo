/*
 * (C) Copyright 2011-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tdelprat
 *
 */
package org.nuxeo.wizard.download;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.5
 */
public class DownloadPackage {

    protected String baseUrl;

    protected String filename;

    protected String label;

    protected String md5;

    /**
     * @since 5.9.3 Virtual package, no download nor install, typically CAP
     */
    protected boolean virtual;

    protected File localFile;

    protected String color;

    protected String textColor;

    protected boolean enabled;

    protected final String id;

    protected String downloadUrl;

    protected String shortLabel;

    /**
     * @since 8.3
     */
    protected String description;

    protected final List<String> impliedDeps = new ArrayList<>();

    public DownloadPackage(String id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        if ("".equals(md5)) {
            md5 = null;
        }
        this.md5 = md5;
    }

    public File getLocalFile() {
        return localFile;
    }

    public void setLocalFile(File localFile) {
        this.localFile = localFile;
    }

    public String getId() {
        return id;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        if (baseUrl != null && !baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        this.baseUrl = baseUrl;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DownloadPackage) {
            return ((DownloadPackage) obj).id.equals(id);
        }
        return super.equals(obj);
    }

    public String getDownloadUrl() {
        if (downloadUrl != null) {
            return downloadUrl;
        }
        return getBaseUrl() + getFilename();
    }

    public void setDownloadUrl(String url) {
        downloadUrl = url;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(id);
        sb.append(" (");
        sb.append(label);
        sb.append(" )]");
        return sb.toString();
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isAlreadyInLocal() {
        return getLocalFile() != null && getLocalFile().exists();
    }

    public List<String> getImpliedDeps() {
        return impliedDeps;
    }

    public void addDep(String depId) {
        impliedDeps.add(depId);
    }

    public void addDeps(String[] depIds) {
        for (String depId : depIds) {
            addDep(depId);
        }
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public void setShortLabel(String shortLabel) {
        this.shortLabel = shortLabel;
    }

    /**
     * @since 5.9.3
     */
    public boolean isVirtual() {
        return virtual;
    }

    /**
     * @since 5.9.3
     */
    public void setVirtual(boolean virtual) {
        this.virtual = virtual;
    }

    /**
     * @since 8.3
     */
    public String getDescription() {
        return description;
    }

    /**
     * @since 8.3
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return true if can only be downloaded from the Marketplace, not from {@link DownloadPackage#getDownloadUrl()}
     * @since 7.1
     */
    public boolean isLaterDownload() {
        return StringUtils.isBlank(getDownloadUrl()) || StringUtils.isEmpty(getFilename());
    }

}

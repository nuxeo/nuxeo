/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     tdelprat
 *
 */
package org.nuxeo.wizard.download;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.5
 */
public class DownloadPackage {

    protected String baseUrl;

    protected String filename;

    protected String label;

    protected String md5;

    protected File localFile;

    protected String color;

    protected String textColor;

    protected boolean enabled;

    protected final String id;

    protected String downloadUrl;

    protected String shortLabel;

    protected boolean alreadyInLocal = false;

    protected final List<String> impliedDeps = new ArrayList<String>();

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
        if (baseUrl!=null && !baseUrl.endsWith("/")) {
            baseUrl = baseUrl+ "/";
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
        this.downloadUrl = url;
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
        return alreadyInLocal;
    }

    public void setAlreadyInLocal(boolean alreadyInLocal) {
        this.alreadyInLocal = alreadyInLocal;
    }

    public List<String> getImpliedDeps() {
        return impliedDeps;
    }

    public void addDep(String depId) {
        impliedDeps.add(depId);
    }

    public void addDeps(String[] depIds) {
        for (String depId: depIds) {
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

}

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

/**
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class DownloadPackage {

    protected String baseUrl;

    protected String filename;

    protected String label;

    protected String md5;

    protected File localFile;

    protected boolean downloading = false;

    protected final String id;

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
        this.md5 = md5;
    }

    public File getLocalFile() {
        return localFile;
    }

    public void setLocalFile(File localFile) {
        this.localFile = localFile;
    }

    public boolean isDownloading() {
        return downloading;
    }

    public void setDownloading(boolean downloading) {
        this.downloading = downloading;
    }

    public String getId() {
        return id;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
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
        return getBaseUrl() + getFilename();
    }

}

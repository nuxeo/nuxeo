/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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

public class PendingDownload {

    protected final DownloadPackage pkg;

    protected PendingDownloadStatus status = PendingDownloadStatus.PENDING;

    protected float expectedLength;

    protected File dowloadingFile;

    public PendingDownload(DownloadPackage pkg) {
        this.pkg = pkg;
    }

    public PendingDownloadStatus getStatus() {
        return status;
    }

    public void setStatus(PendingDownloadStatus status) {
        this.status = status;
    }

    public DownloadPackage getPkg() {
        return pkg;
    }

    public int getProgress() {
        if (expectedLength == 0 || dowloadingFile == null || dowloadingFile.length() == 0) {
            return 0;
        } else {
            return (int) ((dowloadingFile.length() / expectedLength) * 100);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof PendingDownload) {
            return ((PendingDownload) other).getPkg().getId().equals(pkg.getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return pkg.getId().hashCode();
    }

    public void setFile(long expectedLength, File dowloadingFile) {
        this.expectedLength = expectedLength;
        this.dowloadingFile = dowloadingFile;
    }

    public File getDowloadingFile() {
        return dowloadingFile;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PendingDownload ").append(pkg).append(", status=").append(status);
        return builder.toString();
    }

}

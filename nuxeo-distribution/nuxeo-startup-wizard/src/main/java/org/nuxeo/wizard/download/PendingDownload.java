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

public class PendingDownload {

    public static final int PENDING = 0;

    public static final int INPROGRESS = 1;

    public static final int COMPLETED = 2;

    public static final int VERIFICATION = 3;

    public static final int VERIFIED = 4;

    public static final int ABORTED = -1;

    public static final int MISSING = -2;

    public static final int CORRUPTED = -3;

    protected final DownloadPackage pkg;

    protected int status = PENDING;

    protected float expectedLength;

    protected File dowloadingFile;

    public PendingDownload(DownloadPackage pkg) {
        this.pkg = pkg;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public DownloadPackage getPkg() {
        return pkg;
    }

    public int getProgress() {
        if (expectedLength == 0 || dowloadingFile == null
                || dowloadingFile.length() == 0) {
            return 0;
        } else {
            return new Float((dowloadingFile.length() / expectedLength) * 100).intValue();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof PendingDownload) {
            return ((PendingDownload) other).getPkg().getId().equals(
                    pkg.getId());
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

}

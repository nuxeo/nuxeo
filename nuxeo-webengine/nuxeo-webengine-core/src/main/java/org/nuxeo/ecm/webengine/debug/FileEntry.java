/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.debug;

import java.io.File;
import java.util.Date;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FileEntry {

    protected long lastModified;

    protected final File file;

    public FileEntry(File file) {
        this.file = file;
        lastModified = file.lastModified();
    }

    /**
     * Checks if file changed and update internal state if needed.
     */
    public boolean check() {
        long tm = file.lastModified();
        if (tm > lastModified) {
            lastModified = tm;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return file.getAbsolutePath() + " [" + new Date(lastModified) + "]";
    }

}

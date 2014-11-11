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
package org.nuxeo.ecm.webengine.notifier;

import java.io.File;
import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
// FIXME: not used.
public abstract class AbstractFileChangeListener implements FileChangeListener {

    public static final int CREATED = 1;
    public static final int REMOVED = 2;
    public static final int MODIFIED = 3;

    public abstract void fileChanged(File file, int type) throws Exception;

    public void filesCreated(List<File> entries) throws Exception {
        for (File file : entries) {
            fileChanged(file, CREATED);
        }
    }

    public void filesModified(List<File> entries) throws Exception {
        for (File file : entries) {
            fileChanged(file, MODIFIED);
        }
    }

    public void filesRemoved(List<File> entries) throws Exception {
        for (File file : entries) {
            fileChanged(file, REMOVED);
        }
    }

}

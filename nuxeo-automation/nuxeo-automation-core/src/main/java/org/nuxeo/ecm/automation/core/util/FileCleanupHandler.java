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
package org.nuxeo.ecm.automation.core.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.automation.CleanupHandler;

/**
 * Cleanup Handler that takes a list of files and remove them after the operation chain was executed.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FileCleanupHandler implements CleanupHandler {

    protected List<File> files;

    public FileCleanupHandler() {
        files = new ArrayList<File>();
    }

    public FileCleanupHandler(File file) {
        this ();
        files.add(file);
    }

    public FileCleanupHandler(Collection<File> files) {
        this ();
        this.files.addAll(files);
    }

    public void cleanup() throws Exception {
        for (File file : files) {
            file.delete();
        }
    }

}

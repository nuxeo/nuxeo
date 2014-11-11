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
import java.util.ArrayList;
import java.util.List;

/**
 * A file entry that will check all sub directories in the tree rooted in that
 * directory. This will not check the regular files in that tree. For this we
 * may want to use {@link TreeEntry}.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DirectoryEntry extends FileEntry {

    protected List<FileEntry> entries;

    public DirectoryEntry(File file) {
        super(file);
    }

    public List<FileEntry> getChildren() {
        if (entries == null) {
            entries = new ArrayList<FileEntry>();
            collectChildren();
        }
        return entries;
    }

    protected void collectChildren() {
        if (!file.isDirectory()) {
            // may happens for missing directories in a project structure
            return;
        }
        for (File f : file.listFiles()) {
            if (f.isDirectory()) {
                entries.add(new DirectoryEntry(f));
            } else {
                entries.add(new FileEntry(f));
            }
        }
    }

    /**
     * Scan tree for changes and invalidate it if needed.
     */
    @Override
    public boolean check() {
        if (super.check()) { // changed
            entries = null;
            return true;
        }
        for (FileEntry entry : getChildren()) {
            if (entry.check()) {
                return true;
            }
        }
        return false;
    }

}

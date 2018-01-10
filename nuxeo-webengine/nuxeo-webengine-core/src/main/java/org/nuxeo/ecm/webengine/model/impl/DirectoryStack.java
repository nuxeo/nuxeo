/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DirectoryStack {

    private static final Log log = LogFactory.getLog(DirectoryStack.class);

    protected final List<File> dirs;

    public DirectoryStack() {
        dirs = new ArrayList<File>();
    }

    public DirectoryStack(List<File> entries) {
        this();
        dirs.addAll(entries);
    }

    public List<File> getDirectories() {
        return dirs;
    }

    public boolean isEmpty() {
        return dirs.isEmpty();
    }

    public void addDirectory(File dir) throws IOException {
        dirs.add(dir.getCanonicalFile());
    }

    /**
     * Gets the file given its name in this virtual directory.
     * <p>
     * The canonical file is returned if any file is found
     *
     * @param name the file name to lookup
     * @return the file in the canonical form
     * @throws IOException
     */
    public File getFile(String name) throws IOException {
        for (File entry : dirs) {
            File file = new File(entry, name);
            if (file.exists()) {
                return file.getCanonicalFile();
            }
        }
        return null;
    }

    public File[] listFiles() {
        List<File> result = new ArrayList<File>();
        for (File entry : dirs) {
            File[] files = entry.listFiles();
            result.addAll(Arrays.asList(files));
        }
        return result.toArray(new File[result.size()]);
    }

    public File[] listFiles(FileFilter filter) {
        List<File> result = new ArrayList<File>();
        for (File entry : dirs) {
            File[] files = entry.listFiles(filter);
            result.addAll(Arrays.asList(files));
        }
        return result.toArray(new File[result.size()]);
    }

}

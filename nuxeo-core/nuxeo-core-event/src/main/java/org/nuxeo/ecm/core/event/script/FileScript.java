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
package org.nuxeo.ecm.core.event.script;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Script that comes from a file.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FileScript extends Script {

    protected final File file;

    public FileScript(File file) {
        this.file = file;
    }

    public FileScript(String path) {
        this(new File(path));
    }

    @Override
    public String getExtension() {
        return getExtension(file.getPath());
    }

    @Override
    public String getLocation() {
        return file.getAbsolutePath();
    }

    @Override
    public Reader getReader() throws IOException {
        return new FileReader(file);
    }

    @Override
    public Reader getReaderIfModified() throws IOException {
        long tm = file.lastModified();
        if (tm > lastModified) {
            synchronized (this) {
                if (tm > lastModified) {
                    lastModified = tm;
                    return new FileReader(file);
                }
            }
        }
        return null;
    }

}

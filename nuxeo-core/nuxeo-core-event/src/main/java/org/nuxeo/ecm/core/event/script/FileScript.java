/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

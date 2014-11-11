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
package org.nuxeo.ecm.automation.client.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FileBlob extends Blob implements HasFile {

    private static final long serialVersionUID = 1L;

    protected final File file;

    public FileBlob(File file) {
        super(file.getName(), getMimeTypeFromExtension(file.getPath()));
        this.file = file;
    }

    @Override
    public InputStream getStream() throws IOException {
        return new FileInputStream(file);
    }
    
    @Override
    public int getLength() {
        long length = file.length();
        if (length > (long)Integer.MAX_VALUE) {
            return -1;
        }
        return (int)length;
    }

    public File getFile() {
        return file;
    }

    public static String getMimeTypeFromExtension(String path) {
        return "application/octet-stream";
    }

}

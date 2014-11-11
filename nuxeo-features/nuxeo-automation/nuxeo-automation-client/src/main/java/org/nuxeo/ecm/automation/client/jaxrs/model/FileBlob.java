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
package org.nuxeo.ecm.automation.client.jaxrs.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FileBlob extends Blob implements HasFile {

    protected final File file;

    public FileBlob(File file) {
        super(file.getName(), getMimeTypeFromExtension(file.getPath()));
        this.file = file;
    }

    @Override
    public InputStream getStream() throws IOException {
        return new FileInputStream(file);
    }

    public File getFile() {
        return file;
    }

    public static String getMimeTypeFromExtension(String path) {
        return "application/octet-stream";
    }

}

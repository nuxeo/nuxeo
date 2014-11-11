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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.scripting;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class ScriptFile {

    public static final String ROOT_PATH = Framework.getLocalService(WebEngine.class)
            .getRootDirectory().getAbsolutePath();

    File file;
    String ext = "";


    //TODO should remove the typed file name
    public ScriptFile(File file) throws IOException {
        String name = file.getName();
        int p = name.lastIndexOf('.');
        if (p > -1) {
            ext = name.substring(p + 1);
        }
        this.file = file.getCanonicalFile();
    }

    public boolean isTemplate() {
        return "ftl".equals(ext);
    }

    public File getFile() {
        return file;
    }

    public String getExtension() {
        return ext;
    }

    public String getAbsolutePath() {
        return file.getAbsolutePath();
    }

    public String getRelativePath() {
        return file.getAbsolutePath().substring(ROOT_PATH.length());
    }

    public String getFileName() {
        return file.getName();
    }

    public String getURL() throws MalformedURLException {
        return file.toURI().toURL().toExternalForm();
    }

    public URL toURL() throws MalformedURLException {
        return file.toURI().toURL();
    }

    public URI toURI() {
        return file.toURI();
    }

    @Override
    public String toString() {
        return file.toString();
    }

    public long lastModified() {
        return file.lastModified();
    }

    public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }

}

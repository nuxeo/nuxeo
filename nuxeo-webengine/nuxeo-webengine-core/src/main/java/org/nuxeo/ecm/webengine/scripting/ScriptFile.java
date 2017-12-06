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
 */
public final class ScriptFile {

    public static final String ROOT_PATH = Framework.getService(WebEngine.class).getRootDirectory().getAbsolutePath();

    File file;

    String ext = "";

    // TODO should remove the typed file name
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

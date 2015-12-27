/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.event.script;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;

import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class URLScript extends Script {

    protected final URL url;

    protected URLConnection conn;

    public URLScript(URL url) throws IOException {
        this.url = url;
        conn = url.openConnection();
    }

    public URLScript(String location) throws IOException {
        this(new URL(location));
    }

    public URLScript(Bundle bundle, String path) throws IOException {
        this(bundle.getEntry(path));
    }

    @Override
    public String getExtension() {
        return getExtension(url.getPath());
    }

    @Override
    public String getLocation() {
        return url.toExternalForm();
    }

    @Override
    public Reader getReaderIfModified() throws IOException {
        URLConnection conn = url.openConnection();
        long tm = conn.getLastModified();
        if (tm > lastModified) {
            synchronized (this) {
                if (tm > lastModified) {
                    lastModified = tm;
                    return new InputStreamReader(conn.getInputStream());
                }
            }
        }
        return null;
    }

    @Override
    public Reader getReader() throws IOException {
        return new InputStreamReader(conn.getInputStream());
    }

}

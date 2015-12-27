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

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

/**
 * Script that comes from a JAR.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class JARFileScript extends Script {

    protected final URL url;

    protected final File jar;

    public JARFileScript(File jar, URL url) {
        this.url = url;
        this.jar = jar;
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
    public Reader getReader() throws IOException {
        return new InputStreamReader(url.openStream());
    }

    @Override
    public Reader getReaderIfModified() throws IOException {
        long tm = jar.lastModified();
        if (tm > lastModified) {
            synchronized (this) {
                if (tm > lastModified) {
                    lastModified = tm;
                    return new InputStreamReader(url.openStream());
                }
            }
        }
        return null;
    }

}

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
package org.nuxeo.runtime.model;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class URLStreamRef implements StreamRef {

    protected final URL url;

    protected final String name;

    public URLStreamRef(URL url) {
        this(url, url.toString());
    }

    public URLStreamRef(URL url, String name) {
        this.url = url;
        this.name = name;
    }

    @Override
    public String getId() {
        return url.toString();
    }

    @Override
    public InputStream getStream() throws IOException {
        return url.openStream();
    }

    @Override
    public URL asURL() {
        return url;
    }

}

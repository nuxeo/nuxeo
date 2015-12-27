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
package org.nuxeo.ecm.automation.client.jaxrs.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class InputStreamDataSource implements DataSource {

    protected final InputStream in;

    protected final String ctype;

    protected final String name;

    public InputStreamDataSource(InputStream in, String ctype) {
        this(in, ctype, "MultipartRequest");
    }

    public InputStreamDataSource(InputStream in, String ctype, String name) {
        this.in = in;
        this.name = name;
        this.ctype = ctype;
    }

    public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException("data source is not writable");
    }

    public String getName() {
        return name;
    }

    public InputStream getInputStream() throws IOException {
        return in;
    }

    public String getContentType() {
        return ctype;
    }

}

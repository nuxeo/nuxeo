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

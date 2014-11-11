/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stephane Lacoin
 */
package org.nuxeo.runtime.test.protocols.inline;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class InlineURLConnection extends URLConnection {
;
    protected final Object content;

    protected InlineURLConnection(URL url)  {
        super(url);
        try {
            content = InlineURLFactory.newObject(Object.class, url);
        } catch (IOException e) {
            throw new Error("Cannot decode data from " + url, e);
        }
    }

    @Override
    public void connect() throws IOException {
        ;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(content.toString().getBytes());
    }

}

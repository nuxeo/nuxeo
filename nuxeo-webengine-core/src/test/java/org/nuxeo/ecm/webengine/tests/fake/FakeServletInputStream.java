/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.tests.fake;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletInputStream;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;

public class FakeServletInputStream extends ServletInputStream {

    private Blob blob;

    private final InputStream stream;

    public FakeServletInputStream(String data) throws IOException {
        blob = new StringBlob(data);
        stream = blob.getStream();
    }

    public FakeServletInputStream(InputStream in) {
        stream = in;
    }

    public FakeServletInputStream(Blob blob) throws IOException {
        this.blob = blob;
        stream = blob.getStream();
    }

    @Override
    public int read() throws IOException {
        return stream.read();
    }

}

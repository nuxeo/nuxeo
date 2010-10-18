/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.net;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.httpclient.methods.RequestEntity;
import org.nuxeo.ecm.core.storage.sql.Binary;

import org.apache.commons.io.IOUtils;

/**
 * Class defining a {@link RequestEntity} that writes from a {@link Binary}.
 */
public class BinaryRequestEntity implements RequestEntity {

    protected final Binary binary;

    public BinaryRequestEntity(Binary binary) {
        this.binary = binary;
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public void writeRequest(OutputStream out) throws IOException {
        IOUtils.copy(binary.getStream(), out);
        out.flush();
    }

    @Override
    public long getContentLength() {
        return binary.getLength();
    }

    @Override
    public String getContentType() {
        return "application/octet-stream";
    }

}

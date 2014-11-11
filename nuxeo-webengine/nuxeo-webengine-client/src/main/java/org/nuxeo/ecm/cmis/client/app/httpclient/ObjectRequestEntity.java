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
package org.nuxeo.ecm.cmis.client.app.httpclient;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.httpclient.methods.RequestEntity;
import org.nuxeo.ecm.cmis.client.app.SerializationHandler;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ObjectRequestEntity<T> implements RequestEntity {

    protected SerializationHandler<T> handler;
    protected T obj;

    public ObjectRequestEntity(SerializationHandler<T> handler, T obj) {
        this.handler =  handler;
        this.obj = obj;
    }

    public long getContentLength() {
        return -1;
    }

    public String getContentType() {
        return handler.getContentType();
    }

    public boolean isRepeatable() {
        return false;
    }

    public void writeRequest(OutputStream out) throws IOException {
        handler.writeEntity(obj, out);
    }

}

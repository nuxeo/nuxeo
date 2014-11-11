/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.automation.client.model;

import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.ecm.automation.client.Session;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class BlobRef extends Blob {

    private static final long serialVersionUID = 1L;

    protected String ref;

    protected volatile Blob blob;

    protected transient Session session;

    public BlobRef(String ref) {
        this.ref = ref;
    }

    public void attach(Session session) {
        this.session = session;
    }

    public Session session() {
        return session;
    }

    public String getRef() {
        return ref;
    }

    @Override
    public InputStream getStream() throws IOException {
        if (blob == null) {
            synchronized (this) {
                if (blob == null) {
                    try {
                        blob = session.getFile(ref);
                    } catch (IOException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new IOException(e);
                    }
                }
            }
        }
        return blob.getStream();
    }



}

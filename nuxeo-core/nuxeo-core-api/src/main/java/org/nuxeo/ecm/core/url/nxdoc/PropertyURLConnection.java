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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.url.nxdoc;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PropertyURLConnection extends URLConnection {

    protected CoreSession session;
    protected boolean shared;
    protected DocumentModel doc;
    protected Property property;

    public PropertyURLConnection(URL url) {
        super(url);
    }

    protected void doConnect(URL u) throws Exception {
        CoreSession session = null;
        String sid = u.getRef();
        if (sid != null) {
            session = CoreInstance.getInstance().getSession(sid);
        }
        if (session == null) {
            RepositoryManager mgr = Framework.getService(RepositoryManager.class);
            Repository repo = mgr.getRepository(u.getHost());
            session = repo.open();
            shared = false;
        } else {
            shared = true;
        }
    }

    @Override
    public void connect() throws IOException {
        if (connected) {
            return;
        }

        if (!useCaches) {
            // connect to remote manager
        }
        if (ifModifiedSince != 0) {
            //
        }

        try {
            doConnect(url);
            getDocument();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            IOException ee = new IOException("Failed to open document connection");
            ee.initCause(e);
            throw ee;
        }
        connected = true;
    }

    @Override
    public long getLastModified() {
        try {
            connect();
            DocumentPart part = getDocument().getPart("dublincore");
            if (part != null) {
                Calendar cal = (Calendar)part.getValue("modified");
                return cal.getTimeInMillis();
            }
            return -1L;
        } catch (Exception e) {
            return -1L;
        }
    }

    protected InputStream getStream() throws IOException {
        connect();
        getDocument();
        try {
            Object value = property.getValue();
            if (value == null) {
                return new ByteArrayInputStream(new byte[0]);
            }
            if (value instanceof Blob) {
                return ((Blob) value).getStream();
            } else if (value instanceof InputStream) {
                return (InputStream) value;
            } else {
                return new ByteArrayInputStream(value.toString().getBytes());
            }
        } catch (PropertyException e) {
            IOException ee = new IOException("Failed to open get property value: " + url);
            ee.initCause(e);
            throw ee;
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (!shared) {
            return new FilterInputStream(getStream()) {
                @Override
                public void close() throws IOException {
                    super.close();
                    CoreInstance.getInstance().close(session);
                    session = null;
                    connected = false;
                }
            };
        } else {
            return getStream();
        }
    }

    protected DocumentModel getDocument() throws IOException {
        try {
            String path = url.getPath();
            int p = path.indexOf('/');
            if (p == -1) {
                throw new IOException("Invalid Document URL: no xpath specified: "+url);
            }
            String id = path.substring(0, p);
            String xpath = path.substring(p+1);
            DocumentModel doc = session.getDocument(new IdRef(id));
            if (doc == null) {
                throw new FileNotFoundException("Nod document property was found for URL: "+url);
            }
            property = doc.getProperty(xpath);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            IOException ee = new IOException("Failed to open document connection: "+url);
            ee.initCause(e);
            throw ee;
        }

        return doc;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (shared && session != null) {
                CoreInstance.getInstance().close(session);
                session = null;
            }
        } finally {
            super.finalize();
        }
    }

}

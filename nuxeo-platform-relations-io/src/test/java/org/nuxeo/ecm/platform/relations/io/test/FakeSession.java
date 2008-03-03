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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: FakeSession.java 25081 2007-09-18 14:57:22Z atchertchian $
 */

package org.nuxeo.ecm.platform.relations.io.test;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import javax.transaction.xa.XAResource;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.query.Query;
import org.nuxeo.ecm.core.query.QueryException;
import org.nuxeo.ecm.core.query.Query.Type;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.security.SecurityManager;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class FakeSession implements Session {

    public Document getDocumentByUUID(String uuid) throws DocumentException {
        return new FakeDocument(uuid);
    }

    public Repository getRepository() {
        return new FakeRepository();
    }

    // not implemented (useless)

    public void cancel() throws DocumentException {
    }

    public void close() throws DocumentException {
    }

    public Document copy(Document src, Document dst, String name)
            throws DocumentException {
        return null;
    }

    public Document createProxyForVersion(Document parent, Document doc,
            String versionLabel) throws DocumentException {
        return null;
    }

    public Query createQuery(String query, Type type, String... params)
            throws QueryException {
        return null;
    }

    public void dispose() {
    }

    public InputStream getDataStream(String key) throws DocumentException {
        return null;
    }

    public Collection<Document> getProxies(Document doc, Document folder)
            throws DocumentException {
        return null;
    }

    public Document getRootDocument() throws DocumentException {
        return null;
    }

    public SecurityManager getSecurityManager() {
        return null;
    }

    public Map<String, Serializable> getSessionContext() {
        return null;
    }

    public long getSessionId() {
        return 0;
    }

    public SchemaManager getTypeManager() {
        return null;
    }

    public String getUserSessionId() {
        return null;
    }

    public XAResource getXAResource() {
        return null;
    }

    public boolean isLive() {
        return false;
    }

    public Document move(Document src, Document dst, String name)
            throws DocumentException {
        return null;
    }

    public Document resolvePath(String path) throws DocumentException {
        return null;
    }

    public void save() throws DocumentException {
    }

}

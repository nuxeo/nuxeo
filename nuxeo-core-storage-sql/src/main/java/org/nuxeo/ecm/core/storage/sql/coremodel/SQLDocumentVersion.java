/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.io.Serializable;
import java.util.Calendar;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Node;
import org.nuxeo.ecm.core.versioning.DocumentVersion;

/**
 * @author Florent Guillaume
 */
public class SQLDocumentVersion extends SQLDocument implements DocumentVersion {

    public SQLDocumentVersion(Node node, SQLSession session)
            throws StorageException {
        super(node, session);
    }

    @Override
    public boolean isVersion() {
        return true;
    }

    @Override
    public Document getSourceDocument() throws DocumentException {
        String uuid = getVersionableId().toString();
        if (uuid == null) {
            return null;
        }
        return session.getDocumentByUUID(uuid);
    }

    // the path is that of the versionable doc -- if present
    @Override
    public String getPath() throws DocumentException {
        Node versionable = session.getNodeById(getVersionableId());
        if (versionable == null) {
            return null; // TODO return what? error?
        }
        return session.getPath(versionable);
    }

    // the parent is that of the versionable doc -- if present
    @Override
    public Document getParent() throws DocumentException {
        Node versionable = session.getNodeById(getVersionableId());
        if (versionable == null) {
            return null;
        }
        return session.getParent(versionable);
    }

    @Override
    public DocumentVersion getLastVersion() throws DocumentException {
        throw new UnsupportedOperationException("Illegal call on versions");
    }

    private Serializable getVersionableId() throws DocumentException {
        return (Serializable) getProperty(Model.VERSION_VERSIONABLE_PROP).getValue();
    }

    public String getLabel() throws DocumentException {
        return getString(Model.VERSION_LABEL_PROP);
    }

    public String getDescription() throws DocumentException {
        return getString(Model.VERSION_DESCRIPTION_PROP);
    }

    public Calendar getCreated() throws DocumentException {
        return getDate(Model.VERSION_CREATED_PROP);
    }

    public DocumentVersion[] getPredecessors() throws DocumentException {
        throw new UnsupportedOperationException();
    }

    public DocumentVersion[] getSuccessors() throws DocumentException {
        throw new UnsupportedOperationException();
    }

}

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
 *     George Lefter
 *
 * $Id$
 */

package org.nuxeo.ecm.directory.impl;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.BaseSession;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.api.DirectoryClient;
import org.nuxeo.ecm.directory.api.DirectoryManager;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * XXX : this not actually serializable because Bean Proxy is not serializable.
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public class DirectoryClientImpl extends BaseSession implements DirectoryClient {

    private static final long serialVersionUID = -1170479958816244690L;

    private static final Log log = LogFactory.getLog(DirectoryClientImpl.class);

    private final long sessionId;

    private transient DirectoryManager manager;

    public DirectoryClientImpl(long sessionId) {
        this.sessionId = sessionId;
    }

    private DirectoryManager getDirectoryManager() {
        if (manager == null) {
            try {
                manager = (DirectoryManager) Framework.getService(DirectoryService.class);
            } catch (Exception e) {
                log.error("Can't get DirectoryService", e);
                return null;
            }
        }
        return manager;
    }

    public long getSessionId() {
        return sessionId;
    }

    public boolean authenticate(String username, String password)
            throws DirectoryException {
        return getDirectoryManager().authenticate(sessionId, username, password);
    }

    public void close() throws DirectoryException {
        getDirectoryManager().close(sessionId);
    }

    public void commit() throws DirectoryException {
        getDirectoryManager().commit(sessionId);
    }

    public DocumentModel createEntry(Map<String, Object> fieldMap)
            throws DirectoryException {
        return getDirectoryManager().createEntry(sessionId, fieldMap);
    }

    public void deleteEntry(DocumentModel docModel) throws DirectoryException {
        getDirectoryManager().deleteEntry(sessionId, docModel);
    }

    public void deleteEntry(String id) throws DirectoryException {
        getDirectoryManager().deleteEntry(sessionId, id);
    }

    public void deleteEntry(String id, Map<String, String> map)
            throws DirectoryException {
        getDirectoryManager().deleteEntry(sessionId, id, map);
    }

    public DocumentModelList getEntries() throws DirectoryException {
        return getDirectoryManager().getEntries(sessionId);
    }

    public DocumentModel getEntry(String id) throws DirectoryException {
        return getDirectoryManager().getEntry(sessionId, id);
    }

    public DocumentModel getEntry(String id, boolean fetchReferences)
            throws DirectoryException {
        return getDirectoryManager().getEntry(sessionId, id, fetchReferences);
    }

    public String getIdField() throws DirectoryException {
        return getDirectoryManager().getIdField(sessionId);
    }

    public String getPasswordField() throws DirectoryException {
        return getDirectoryManager().getPasswordField(sessionId);
    }

    public List<String> getProjection(Map<String, Serializable> filter,
            String columnName) throws DirectoryException {
        return getDirectoryManager().getProjection(sessionId, filter,
                columnName);
    }

    public List<String> getProjection(Map<String, Serializable> filter,
            Set<String> fulltext, String columnName) throws DirectoryException {
        return getDirectoryManager().getProjection(sessionId, filter,
                columnName);
    }

    public boolean isAuthenticating() throws DirectoryException {
        return getDirectoryManager().isAuthenticating(sessionId);
    }

    public boolean isReadOnly() throws DirectoryException {
        return getDirectoryManager().isReadOnly(sessionId);
    }

    public DocumentModelList query(Map<String, Serializable> filter)
            throws DirectoryException {
        return getDirectoryManager().query(sessionId, filter);
    }

    public DocumentModelList query(Map<String, Serializable> filter,
            Set<String> fulltext) throws DirectoryException {
        return getDirectoryManager().query(sessionId, filter, fulltext);
    }

    public DocumentModelList query(Map<String, Serializable> filter,
            Set<String> fulltext, Map<String, String> orderBy)
            throws DirectoryException {
        return getDirectoryManager().query(sessionId, filter, fulltext, orderBy);
    }

    public DocumentModelList query(Map<String, Serializable> filter,
            Set<String> fulltext, Map<String, String> orderBy,
            boolean fetchReferences) throws DirectoryException {
        return getDirectoryManager().query(sessionId, filter, fulltext,
                orderBy, fetchReferences);
    }

    public void rollback() throws DirectoryException {
        getDirectoryManager().rollback(sessionId);
    }

    public void updateEntry(DocumentModel docModel) throws DirectoryException {
        getDirectoryManager().updateEntry(sessionId, docModel);
    }

    public DocumentModel createEntry(DocumentModel entry)
            throws ClientException {
        return getDirectoryManager().createEntry(sessionId, entry);
    }

    public boolean hasEntry(String id) throws ClientException {
        return getDirectoryManager().hasEntry(sessionId, id);
    }

}

/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.directory.ejb;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.DirectoryFactory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryManager;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.impl.DirectoryClientImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Facade Bean provide Remoting API for DirectoryService
 *
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 * @author <a href="mailto:ogrisel@nuxeo.com">Olivier Grisel</a>
 */
@Stateless
@Remote(DirectoryManager.class)
@Local(DirectoryManagerLocal.class)
public class DirectoryManagerBean implements DirectoryManagerLocal {

    private static final Map<Long, Session> sessionMap = new ConcurrentHashMap<Long, Session>();

    private static final Map<Long, String> sessionDirectoryNames = new ConcurrentHashMap<Long, String>();

    private static final AtomicLong sessionIdCounter = new AtomicLong(0);

    private DirectoryService directoryService;

    @PostConstruct
    public void initialize() {
        getService();
    }

    private DirectoryService getService() {
        if (directoryService == null) {
            directoryService = Framework.getLocalService(DirectoryService.class);
        }
        return directoryService;
    }

    private Session getSession(long sessionId) throws DirectoryException {
        Session session = sessionMap.get(sessionId);
        if (session == null) {
            String directoryName = sessionDirectoryNames.get(sessionId);
            if (directoryName == null) {
                throw new DirectoryException(
                        "Could not find directory name while rebuilding session with id: "
                                + sessionId);
            }
            session = getService().open(directoryName);
        }
        return session;
    }

    public boolean authenticate(long sessionId, String username, String password)
            throws DirectoryException {
        try {
            return getSession(sessionId).authenticate(username, password);
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public void close(long sessionId) throws DirectoryException {
        try {
            getSession(sessionId).close();
            sessionMap.remove(sessionId);
            sessionDirectoryNames.remove(sessionId);
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public void commit(long sessionId) throws DirectoryException {
        try {
            getSession(sessionId).commit();
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public DocumentModel createEntry(long sessionId, Map<String, Object> map)
            throws DirectoryException {
        try {
            return getSession(sessionId).createEntry(map);
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public void deleteEntry(long sessionId, DocumentModel docModel)
            throws DirectoryException {
        try {
            getSession(sessionId).deleteEntry(docModel);
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public void deleteEntry(long sessionId, String id)
            throws DirectoryException {
        try {
            getSession(sessionId).deleteEntry(id);
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public void deleteEntry(long sessionId, String id, Map<String, String> map)
            throws DirectoryException {
        try {
            getSession(sessionId).deleteEntry(id, map);
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public DocumentModelList getEntries(long sessionId)
            throws DirectoryException {
        try {
            return getSession(sessionId).getEntries();
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public DocumentModel getEntry(long sessionId, String id)
            throws DirectoryException {
        try {
            return getSession(sessionId).getEntry(id);
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public DocumentModel getEntry(long sessionId, String id,
            boolean fetchReferences) throws DirectoryException {
        try {
            return getSession(sessionId).getEntry(id, fetchReferences);
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public String getIdField(long sessionId) throws DirectoryException {
        try {
            return getSession(sessionId).getIdField();
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public String getPasswordField(long sessionId) throws DirectoryException {
        try {
            return getSession(sessionId).getPasswordField();
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public List<String> getProjection(long sessionId,
            Map<String, Serializable> filter, String columnName)
            throws DirectoryException {
        try {
            return getSession(sessionId).getProjection(filter, columnName);
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public List<String> getProjection(long sessionId,
            Map<String, Serializable> filter, Set<String> fulltext,
            String columnName) throws DirectoryException {
        try {
            return getSession(sessionId).getProjection(filter, fulltext,
                    columnName);
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public boolean isAuthenticating(long sessionId) throws DirectoryException {
        try {
            return getSession(sessionId).isAuthenticating();
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public boolean isReadOnly(long sessionId) throws DirectoryException {
        try {
            return getSession(sessionId).isReadOnly();
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public DocumentModelList query(long sessionId,
            Map<String, Serializable> filter) throws DirectoryException {
        try {
            return getSession(sessionId).query(filter);
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public DocumentModelList query(long sessionId,
            Map<String, Serializable> filter, Set<String> fulltext)
            throws DirectoryException {
        try {
            return getSession(sessionId).query(filter, fulltext);
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public DocumentModelList query(long sessionId,
            Map<String, Serializable> filter, Set<String> fulltext,
            Map<String, String> orderBy) throws DirectoryException {
        try {
            return getSession(sessionId).query(filter, fulltext, orderBy);
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public DocumentModelList query(long sessionId,
            Map<String, Serializable> filter, Set<String> fulltext,
            Map<String, String> orderBy, boolean fetchReferences)
            throws DirectoryException {
        try {
            return getSession(sessionId).query(filter, fulltext, orderBy,
                    fetchReferences);
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public void rollback(long sessionId) throws DirectoryException {
        try {
            getSession(sessionId).rollback();
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public void updateEntry(long sessionId, DocumentModel docModel)
            throws DirectoryException {
        try {
            getSession(sessionId).updateEntry(docModel);
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public List<String> getDirectoryNames() throws DirectoryException {
        try {
            return getService().getDirectoryNames();
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public String getDirectorySchema(String directoryName)
            throws DirectoryException {
        try {
            return getService().getDirectorySchema(directoryName);
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public Session open(String directoryName) throws DirectoryException {
        try {
            Session session = getService().open(directoryName);
            long id = sessionIdCounter.incrementAndGet();
            sessionMap.put(id, session);
            sessionDirectoryNames.put(id, directoryName);
            return new DirectoryClientImpl(id);
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public Directory getDirectory(String name) throws DirectoryException {
        throw new DirectoryException("Directories cannot be remotely retrieved");
    }

    public List<Directory> getDirectories() throws DirectoryException {
        throw new DirectoryException("Directories cannot be remotely retrieved");
    }

    public String getDirectoryIdField(String directoryName)
            throws DirectoryException {
        return getService().getDirectoryIdField(directoryName);
    }

    public String getDirectoryPasswordField(String directoryName)
            throws DirectoryException {
        return getService().getDirectoryPasswordField(directoryName);
    }

    public void registerDirectory(String directoryName, DirectoryFactory factory) {
        getService().registerDirectory(directoryName, factory);
    }

    public void unregisterDirectory(String directoryName,
            DirectoryFactory factory) {
        getService().unregisterDirectory(directoryName, factory);
    }

    public String getParentDirectoryName(String directoryName)
            throws DirectoryException {
        return getService().getParentDirectoryName(directoryName);
    }

    public DocumentModel createEntry(long sessionId, DocumentModel entry)
            throws DirectoryException {
        try {
            return getSession(sessionId).createEntry(entry);
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

    public boolean hasEntry(long sessionId, String id)
            throws DirectoryException {
        try {
            return getSession(sessionId).hasEntry(id);
        } catch (Throwable e) {
            throw DirectoryException.wrap(e);
        }
    }

}

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
 *     Florent Guillaume
 *
 * $Id: MultiDirectory.java 25713 2007-10-05 16:06:58Z fguillaume $
 */

package org.nuxeo.ecm.directory.multi;

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.directory.AbstractDirectory;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.IdGenerator;
import org.nuxeo.ecm.directory.Reference;
import org.nuxeo.ecm.directory.Session;

/**
 * @author Florent Guillaume
 *
 */
public class MultiDirectory extends AbstractDirectory {

    private final MultiDirectoryDescriptor descriptor;

    private Set<MultiDirectorySession> sessions;

    public MultiDirectory(MultiDirectoryDescriptor descriptor) {
        this.descriptor = descriptor;
        sessions = new HashSet<MultiDirectorySession>();
    }

    protected MultiDirectoryDescriptor getDescriptor() {
        return descriptor;
    }

    public IdGenerator getIdGenerator() {
        return null; // this method is never called
    }

    public String getName() {
        return descriptor.name;
    }

    public String getSchema() {
        return descriptor.schemaName;
    }

    public String getParentDirectory() {
        return null; // no parent directories are specified for multi
    }

    public String getIdField() {
        return descriptor.idField;
    }

    public String getPasswordField() {
        return descriptor.passwordField;
    }

    public Session getSession() throws DirectoryException {
        MultiDirectorySession session = new MultiDirectorySession(this);
        sessions.add(session);
        return session;
    }

    /**
     * Called from a session's close()
     */
    protected void removeSession(Session session) {
        sessions.remove(session);
    }

    public void shutdown() throws DirectoryException {
        try {
            // use toArray to avoid concurrent modification of list
            for (Object session : sessions.toArray()) {
                ((Session) session).close();
            }
            sessions = null;
        } catch (ClientException e) {
            throw new DirectoryException(e);
        }
    }

    @Override
    public Reference getReference(String referenceFieldName) {
        return new MultiReference(this, referenceFieldName);
    }

    @Override
    public void invalidateDirectoryCache() throws DirectoryException {
        getCache().invalidateAll();
        // and also invalidates the cache from the source directories
        for (SourceDescriptor src : descriptor.sources) {
            for (SubDirectoryDescriptor sub : src.subDirectories) {
                Directory dir = MultiDirectoryFactory.getDirectoryService().getDirectory(
                        sub.name);
                if (dir != null) {
                    dir.invalidateDirectoryCache();
                }
            }
        }
    }

}

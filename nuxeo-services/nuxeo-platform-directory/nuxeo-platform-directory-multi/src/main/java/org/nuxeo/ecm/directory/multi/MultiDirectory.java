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

import org.nuxeo.ecm.directory.AbstractDirectory;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Reference;
import org.nuxeo.ecm.directory.Session;

/**
 * @author Florent Guillaume
 *
 */
public class MultiDirectory extends AbstractDirectory {

    private final MultiDirectoryDescriptor descriptor;

    public MultiDirectory(MultiDirectoryDescriptor descriptor) {
        super(descriptor.name);
        this.descriptor = descriptor;
    }

    protected MultiDirectoryDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public String getName() {
        return descriptor.name;
    }

    @Override
    public String getSchema() {
        return descriptor.schemaName;
    }

    @Override
    public String getParentDirectory() {
        return null; // no parent directories are specified for multi
    }

    @Override
    public String getIdField() {
        return descriptor.idField;
    }

    @Override
    public String getPasswordField() {
        return descriptor.passwordField;
    }

    @Override
    public Session getSession() throws DirectoryException {
        MultiDirectorySession session = new MultiDirectorySession(this);
        addSession(session);
        return session;
    }

    protected void addSession(MultiDirectorySession session) {
        sessions.add(session);
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

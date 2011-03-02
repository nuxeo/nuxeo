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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.directory.sql;

import java.util.Collection;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryCache;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.IdGenerator;
import org.nuxeo.ecm.directory.Reference;
import org.nuxeo.ecm.directory.Session;

/**
 * Lazy directory initialization.
 *
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class SQLDirectoryProxy implements Directory {

    private final SQLDirectoryDescriptor descriptor;
    private SQLDirectory directory;

    public SQLDirectoryProxy(SQLDirectoryDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public SQLDirectory getDirectory() throws DirectoryException {
        // this method is public to make it easier to test
        if (directory == null) {
            try {
                directory = new SQLDirectory(descriptor);
            } catch (ClientException e) {
                throw new DirectoryException(e);
            }
        }
        return directory;
    }

    @Override
    public IdGenerator getIdGenerator() throws DirectoryException {
        return getDirectory().getIdGenerator();
    }

    @Override
    public String getName() throws DirectoryException {
        return getDirectory().getName();
    }

    @Override
    public String getSchema() throws DirectoryException {
        return getDirectory().getSchema();
    }

    @Override
    public String getParentDirectory() throws DirectoryException {
        return getDirectory().getParentDirectory();
    }

    @Override
    public String getIdField() throws DirectoryException {
        return getDirectory().getIdField();
    }

    @Override
    public String getPasswordField() throws DirectoryException {
        return getDirectory().getPasswordField();
    }

    @Override
    public Session getSession() throws DirectoryException {
        return getDirectory().getSession();
    }

    @Override
    public void shutdown() throws DirectoryException {
        // This is called when unregistering extensions, no need
        // to create a directory just to shut it down.
        if (directory != null) {
            getDirectory().shutdown();
        }
    }

    @Override
    public Reference getReference(String referenceFieldName) throws DirectoryException {
        return getDirectory().getReference(referenceFieldName);
    }

    @Override
    public Collection<Reference> getReferences() throws DirectoryException {
        return getDirectory().getReferences();
    }

    public SQLDirectoryDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public DirectoryCache getCache() throws DirectoryException {
        return getDirectory().getCache();
    }

    @Override
    public void invalidateDirectoryCache() throws DirectoryException{
        getCache().invalidateAll();
    }

}

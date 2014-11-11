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

package org.nuxeo.ecm.directory.ldap;

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
 * @author <a href="mailto:ogrisel@nuxeo.com">Olivier Grisel</a>
 */
public class LDAPDirectoryProxy implements Directory {

    private final LDAPDirectoryDescriptor descriptor;
    private LDAPDirectory directory;

    public LDAPDirectoryProxy(LDAPDirectoryDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public LDAPDirectory getDirectory() throws DirectoryException {
        if (null == directory) {
            try {
                directory = new LDAPDirectory(descriptor);
            } catch (DirectoryException e) {
                throw e;
            } catch (ClientException e) {
                throw new DirectoryException(e);
            }
        }
        return directory;
    }

    public IdGenerator getIdGenerator() throws DirectoryException {
        return getDirectory().getIdGenerator();
    }

    public String getName() throws DirectoryException {
        return getDirectory().getName();
    }

    public String getSchema() throws DirectoryException {
        return getDirectory().getSchema();
    }

    public String getParentDirectory() throws DirectoryException {
        return getDirectory().getParentDirectory();
    }

    public String getIdField() throws DirectoryException {
        return getDirectory().getIdField();
    }

    public String getPasswordField() throws DirectoryException {
        return getDirectory().getPasswordField();
    }

    public Session getSession() throws DirectoryException {
        return getDirectory().getSession();
    }

    public void shutdown() throws DirectoryException {
        if (directory != null) {
            directory.shutdown();
        }
    }

    public Reference getReference(String referenceFieldName) {
        try {
            return getDirectory().getReference(referenceFieldName);
        } catch (DirectoryException e) {
            return null;
        }
    }

    public Collection<Reference> getReferences() throws DirectoryException {
        return getDirectory().getReferences();
    }

    public DirectoryCache getCache() throws DirectoryException {
        return getDirectory().getCache();
    }

    public void invalidateDirectoryCache() throws DirectoryException{
        getCache().invalidateAll();
    }
}

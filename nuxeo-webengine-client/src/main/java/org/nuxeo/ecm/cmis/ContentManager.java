/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.cmis;

import org.nuxeo.ecm.client.NoSuchRepositoryException;

/**
 * The entry point to CMIS repositories exposed by a server.
 * 
 * An instance of this service can be used to enumerate available repositories, and fetch repository objects given they name.
 * This interface doesn't define how to register new repositories.
 * <p>
 * There will be different implementations depending on how the repositories are accessed i.e. 
 * whether or not the ContentManager is used to access remote or local repositories.
 * <p>
 * The discovery mechanism used by the implementation to detect repositories is up to the implementors.
 * Repositories connected through APP will use APP discovery, local repositories will use repository specific java API etc.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface ContentManager {
    
    Repository[] getRepositories() throws ContentManagerException;
    
    Repository getDefaultRepository() throws ContentManagerException;
    
    Repository getRepository(String id) throws NoSuchRepositoryException, ContentManagerException;
    
}

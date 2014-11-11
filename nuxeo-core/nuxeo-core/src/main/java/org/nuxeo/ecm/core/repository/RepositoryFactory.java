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

package org.nuxeo.ecm.core.repository;

import org.nuxeo.ecm.core.model.Repository;



/**
 * Repository Factory.
 * <p>
 * Creates a repository and optionally an JNDI bindable object
 * to bind the repository to a JNDI name.
 * <p>
 * The repository factory to use is usually specified in the MBean configuration file.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface RepositoryFactory {

    /**
     * Creates a repository implementation given a repository descriptor object.
     *
     * @param descriptor the repository descriptor
     * @return the repository instance
     * @throws Exception if an error occurs
     */
    Repository createRepository(RepositoryDescriptor descriptor) throws Exception;

}

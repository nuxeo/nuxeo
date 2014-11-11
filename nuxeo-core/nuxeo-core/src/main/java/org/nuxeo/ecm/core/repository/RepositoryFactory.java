/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

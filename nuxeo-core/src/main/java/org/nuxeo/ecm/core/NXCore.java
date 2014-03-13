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

package org.nuxeo.ecm.core;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.core.lifecycle.impl.LifeCycleServiceImpl;
import org.nuxeo.ecm.core.model.NoSuchRepositoryException;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.runtime.api.Framework;

/**
 * CoreSession facade for services provided by NXCore module.
 * <p>
 * This is the main entry point to the core services.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class NXCore {

    private NXCore() {
    }

    /**
     * Returns the life cycle service.
     *
     * @see LifeCycleServiceImpl
     *
     * @return the life cycle service
     */
    public static LifeCycleService getLifeCycleService() {
        return (LifeCycleService) Framework.getRuntime().getComponent(
                LifeCycleServiceImpl.NAME);
    }

    /**
     * Not a public API.
     * @deprecated not public
     * @since 5.9.3
     */
    @Deprecated
    public static RepositoryService getRepositoryService() {
        return Framework.getLocalService(RepositoryService.class);
    }

    public static Repository getRepository(String name)
            throws NoSuchRepositoryException {
        try {
//            needed by glassfish
//            return (Repository) new InitialContext()
//                .lookup("NXRepository/" + name);
            return (Repository) new InitialContext()
                    .lookup("java:NXRepository/" + name);
        } catch (NamingException e) {
            throw new NoSuchRepositoryException("Failed to lookup repository: "
                    + name, e);
        }
    }

    public static SecurityService getSecurityService() {
        return (SecurityService) Framework.getRuntime().getComponent(
                SecurityService.NAME);
    }

}

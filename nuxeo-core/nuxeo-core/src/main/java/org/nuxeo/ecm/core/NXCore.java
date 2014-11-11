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

package org.nuxeo.ecm.core;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.nuxeo.ecm.core.lifecycle.LifeCycleService;
import org.nuxeo.ecm.core.lifecycle.impl.LifeCycleServiceImpl;
import org.nuxeo.ecm.core.listener.CoreEventListenerService;
import org.nuxeo.ecm.core.listener.impl.CoreEventListenerServiceImpl;
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

    public static RepositoryService getRepositoryService() {
        return (RepositoryService) Framework.getRuntime().getComponent(
                RepositoryService.NAME);
    }

    /**
     * Returns the core event listener service.
     *
     * @see CoreEventListenerServiceImpl
     *
     * @return the core event listener service
     */
    public static CoreEventListenerService getCoreEventListenerService() {
        return Framework.getLocalService(CoreEventListenerService.class);
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

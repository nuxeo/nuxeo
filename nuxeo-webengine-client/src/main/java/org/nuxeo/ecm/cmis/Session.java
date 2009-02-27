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



/**
 * A session is used by a client to access a repository.
 * 
 * The session provide a way to get the root of the repositories and all the other services defined by CMIS.
 * Also, this interface enable specific implementation to expose their own services through {@link #getService(Class)} 
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface Session {
    
    Repository getRepository();
    
    DocumentEntry getRoot();
    
    DiscoveryService getDiscoveryService();

    NavigationService getNavigationService();

    ObjectService getObjectService();
    
    //TODO add the rest of services

    /**
     * Get a custom service given its interface.
     * Return null if no such service exists.
     * This can be used to extend the set of default CMIS services with custom services.
     * 
     * @param <T>
     * @param serviceType the service interface
     * @return the service or null if no such service exists
     */
    <T> T getService(Class<T> serviceType);
    
}

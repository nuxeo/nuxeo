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
 *     matic
 */
package org.nuxeo.ecm.client;

import java.util.List;


/**
 * Repository is mapped to an atom service or a workspace in
 *
 * @author matic
 *
 */
public interface Repository  {

    String getRepositoryId();

    DocumentEntry getRoot();

    List<QueryEntry> getQueries() throws CannotConnectToServerException;

    DiscoveryService getDiscoveryService();

    NavigationService getNavigationService();

    ObjectService getObjectService();

    <T> T getService(Class<T> serviceType);

}

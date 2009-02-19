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

/**
 * Exposes the Repository Service has defined in CMIS Spec Part I
 * 
 * @author matic
 * 
 */
public interface RepositoryService {

    Repository[] getRepositories() throws CannotConnectToServerException;
    
    Repository getDefaultRepository() throws CannotConnectToServerException;
    
    Repository getRepository(String id) throws NoSuchRepositoryException, CannotConnectToServerException;

}

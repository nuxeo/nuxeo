/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     mcedica
 */
package org.nuxeo.ecm.management.administrativestatus.service;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;


/**
 * Used to control the server administrative status: the status of the server
 * can be locked/unlocked
 * 
 * @author Mariana Cedica
 */
public interface AdministrativeStatusService {

    /**
     * Locks the server
     * 
     * @return true if the locked succeed
     */
    boolean lockServer(CoreSession session) throws ClientException;

    /**
     * Unlocks the server
     * 
     * @return true if the unlocked succeed
     */
    boolean unlockServer(CoreSession session) throws ClientException;

    
    /**
     * @return the server status
     */
    String getServerStatus(CoreSession session) throws ClientException;

}

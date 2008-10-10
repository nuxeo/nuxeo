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
 *     anguenot
 *
 * $Id: BaseNuxeoWebService.java 19483 2007-05-27 10:52:56Z sfermigier $
 */

package org.nuxeo.ecm.platform.api.ws;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * Base Nuxeo web service interface.
 * <p>
 * Defines the base API dealing with Web Service remote sessions.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface BaseNuxeoWebService extends Serializable {

    /**
     * Connects to the default nuxeo core repository.
     *
     * @param username the user name
     * @param password the user password
     * @return a Nuxeo core session identifier.
     */
    String connect(String username, String password)
            throws ClientException;

    /**
     * Connects to a given nuxeo core repository.
     *
     * @param username the user name.
     * @param password the user password
     * @param repository the repository name.
     * @return a Nuxeo core session identifier.
     */
    /*
    String connect(String username, String password, String repository)
            throws ClientException;
    */


    /**
     * Disconnect the Nuxeo core given the session id.
     *
     * @param sid the Nuxeo core session id.
     * @throws ClientException
     */
    void disconnect(String sid) throws ClientException;

}

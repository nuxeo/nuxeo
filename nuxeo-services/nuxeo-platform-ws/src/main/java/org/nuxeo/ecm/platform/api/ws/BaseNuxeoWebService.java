/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     anguenot
 *
 * $Id: BaseNuxeoWebService.java 19483 2007-05-27 10:52:56Z sfermigier $
 */

package org.nuxeo.ecm.platform.api.ws;

import java.io.Serializable;


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
    String connect(String username, String password);

    /**
     * Connects to a given nuxeo core repository.
     *
     * @param username the user name.
     * @param password the user password
     * @param repository the repository name.
     * @return a Nuxeo core session identifier.
     */
    /*
     * String connect(String username, String password, String repository);
     */

    /**
     * Disconnect the Nuxeo core given the session id.
     *
     * @param sid the Nuxeo core session id.
     */
    void disconnect(String sid);

}

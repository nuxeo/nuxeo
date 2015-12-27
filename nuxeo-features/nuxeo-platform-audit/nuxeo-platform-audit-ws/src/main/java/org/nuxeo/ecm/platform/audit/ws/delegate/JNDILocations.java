/*
 * (C) Copyright 2006 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 *
 * $Id: JNDILocations.java 17822 2007-04-26 04:46:23Z janguenot $
 */

package org.nuxeo.ecm.platform.audit.ws.delegate;

/**
 * Holds JNDI locations of the EJBs.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class JNDILocations {

    public static final String nxauditWsAuditLocalLocation = "nuxeo/WSAuditBean/local";

    public static final String nxauditWsAuditRemoteLocation = "nuxeo/WSAuditBean/remote";

    private JNDILocations() {
    }

}

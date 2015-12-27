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
 *     Nuxeo - initial API and implementation
 *
 * $Id:ContentHistoryBusinessDelegate.java 3895 2006-10-11 19:12:47Z janguenot $
 */

package org.nuxeo.ecm.platform.audit.ws.delegate;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.audit.ws.api.WSAudit;

/**
 * EJB Factory.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class EJBFactory {

    private static final Log log = LogFactory.getLog(EJBFactory.class);

    // Utility class.
    private EJBFactory() {
    }

    private static InitialContext getInitialContext() throws NamingException {
        return new InitialContext();
    }

    public static WSAudit getWSAuditRemote() throws NamingException {
        String beanRemoteLocation = JNDILocations.nxauditWsAuditRemoteLocation;
        if (log.isDebugEnabled()) {
            log.debug("Trying to get the remote EJB with JNDI location :" + beanRemoteLocation);
        }
        InitialContext ctx = getInitialContext();
        return (WSAudit) ctx.lookup(beanRemoteLocation);
    }

}

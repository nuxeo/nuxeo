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
            log.debug("Trying to get the remote EJB with JNDI location :"
                    + beanRemoteLocation);
        }
        InitialContext ctx = getInitialContext();
        return (WSAudit) ctx.lookup(beanRemoteLocation);
    }

}

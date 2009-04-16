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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.versioning.ejb;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * @deprecated ejb lookup is not done by hand anymore. remove in 5.2
 */
@Deprecated
public final class EjbLocator {

    public static final String documentManagerRemote = "DocumentManagerBean/remote";

    private static final Log log = LogFactory.getLog(EjbLocator.class);

    // Utility class.
    private EjbLocator() {
    }

    /**
     * Returns a remote document manager bean.
     *
     * @return a DocumentManager bean instance
     * @throws NamingException
     */
    public static CoreSession getDocumentManager() throws NamingException {
        String beanRemoteLocation = EjbLocator.documentManagerRemote;
        log.debug("Trying to get the remote EJB with JNDI location :"
                + beanRemoteLocation);
        InitialContext ctx = new InitialContext();
        return (CoreSession) ctx.lookup(beanRemoteLocation);
    }

}

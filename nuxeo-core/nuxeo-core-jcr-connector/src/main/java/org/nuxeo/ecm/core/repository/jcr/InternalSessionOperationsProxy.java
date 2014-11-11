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

package org.nuxeo.ecm.core.repository.jcr;

import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Proxy to access the internal [jcr] session and perform operations not
 * available on a higher level (JCRSession).
 *
 * @author DM
 *
 */
public final class InternalSessionOperationsProxy {

    /**
     * Logger instance.
     */
    private static final Log log = LogFactory.getLog(InternalSessionOperationsProxy.class);

    // This is an utility class.
    private InternalSessionOperationsProxy() { }

    /**
     * @see javax.jcr.Workspace#copy
     *
     * @param session
     * @param nodePath
     * @param toPath
     * @throws RepositoryException
     */
    public static void copy(JCRSession session, String nodePath, String toPath)
            throws RepositoryException {
        session.getSession().getWorkspace().copy(nodePath, toPath);
    }

}

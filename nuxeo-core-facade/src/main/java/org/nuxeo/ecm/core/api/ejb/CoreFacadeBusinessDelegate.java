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
 * $Id$
 */

package org.nuxeo.ecm.core.api.ejb;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.CoreSessionFactory;

/**
 * Knows how to acquire a reference to a {@link CoreSession}.
 *
 * @author Razvan Caraghin
 *
 * @deprecated use ECM.getPlatform().openRepository(...) instead
 * must be removed but there are yet tests that depends on it
 */
@Deprecated
public class CoreFacadeBusinessDelegate implements CoreSessionFactory,
        Serializable {

    private static final long serialVersionUID = -3747397825256725506L;

    private static final Log log = LogFactory.getLog(CoreFacadeBusinessDelegate.class);

    protected JNDILookupHelper jndiHelper;

    /**
     * Receives the path for the configuration files.
     *
     * @param path
     * @throws ClientException
     */
    public CoreFacadeBusinessDelegate(String path) throws ClientException {
        jndiHelper = new JNDILookupHelper(path);
    }

    public CoreSession getSession() {
        CoreSession documentManager = null;

        try {
            documentManager = (CoreSession) jndiHelper
                    .lookupEjbReference("DocumentManager");
            log.debug("New Document EJB reference acquired...");
        } catch (ClientException e) {
            log.error("Eating the exception...");
            // TODO: do nothing. need to change the API
        }

        return documentManager;
    }

}

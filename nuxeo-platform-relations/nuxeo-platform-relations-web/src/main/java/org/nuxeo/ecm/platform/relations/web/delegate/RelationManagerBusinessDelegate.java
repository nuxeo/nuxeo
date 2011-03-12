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
 * $Id: RelationManagerBusinessDelegate.java 21750 2007-07-02 13:14:50Z atchertchian $
 */

package org.nuxeo.ecm.platform.relations.web.delegate;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@Name("relationManager")
@Scope(SESSION)
public class RelationManagerBusinessDelegate implements Serializable {

    private static final long serialVersionUID = -4778456059717447736L;

    private static final Log log = LogFactory.getLog(RelationManagerBusinessDelegate.class);

    protected RelationManager relationManager;

    // @Create
    @Deprecated
    public void initialize() {
    }

    /**
     * Acquires a new {@link RelationManager} reference. The related EJB may be
     * deployed on a local or remote AppServer.
     *
     * @return
     * @throws ClientException
     */
    @Unwrap
    public RelationManager getRelationManager() throws ClientException {
        if (relationManager == null) {
            try {
                relationManager = Framework.getService(RelationManager.class);
            } catch (Exception e) {
                final String errMsg = "Error connecting to RelationsManager. "
                        + e.getMessage();
                throw new ClientException(errMsg, e);
            }
            if (relationManager == null) {
                throw new ClientException("RelationManager service not bound");
            }

        }
        return relationManager;
    }

    @Destroy
    public void destroy() {
        if (null != relationManager) {
            relationManager = null;
        }
        log.debug("Destroyed the seam component");
    }

}

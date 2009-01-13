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

package org.nuxeo.ecm.webapp.delegate;

import java.io.Serializable;

import javax.annotation.security.PermitAll;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Business delegate for VersioningManager service.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
@Name("versioningManager")
@Scope(ScopeType.CONVERSATION)
public class DocumentVersioningBusinessDelegate implements Serializable {

    private static final long serialVersionUID = -3782178155516634239L;

    private static final Log log = LogFactory.getLog(DocumentVersioningBusinessDelegate.class);

    protected VersioningManager versioningManager;

    //@Create
    public void initialize() {
        log.debug("Seam component initialized...");
    }

    /**
     * Acquires a new {@link VersioningManager} reference. The related EJB may
     * be deployed on a local or remote AppServer.
     */
    @Unwrap
    public VersioningManager getVersioningManager() throws ClientException {
        if (null == versioningManager) {
            try {
                versioningManager = Framework.getService(VersioningManager.class);
            } catch (Exception e) {
                final String errMsg = "Error connecting to VersioningManager. "
                        + e.getMessage();
                throw new ClientException(errMsg, e);
            }

            if (null == versioningManager) {
                throw new ClientException("VersioningManager service not bound");
            }
        }

        return versioningManager;
    }

    @Destroy
    @PermitAll
    public void destroy() {
        log.debug("Destroyed the seam component...");
    }

}

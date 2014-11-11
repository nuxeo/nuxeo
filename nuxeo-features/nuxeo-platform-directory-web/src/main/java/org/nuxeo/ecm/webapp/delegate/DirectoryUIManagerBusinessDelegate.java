/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.webapp.delegate;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.directory.api.ui.DirectoryUIManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Directory UI manager business delegate
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@Name("directoryUIManager")
@Scope(SESSION)
public class DirectoryUIManagerBusinessDelegate implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DirectoryUIManagerBusinessDelegate.class);

    protected DirectoryUIManager service;

    public void initialize() {
        log.debug("Seam component initialized...");
    }

    /**
     * Acquires a new {@link DirectoryUIManager} reference. The related EJB may
     * be deployed on a local or remote AppServer.
     */
    @Unwrap
    public DirectoryUIManager getVocabularyUIManager() throws ClientException {
        if (service == null) {
            try {
                service = Framework.getService(DirectoryUIManager.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }
            if (service == null) {
                throw new ClientException("Service not bound");
            }

        }
        return service;
    }

    @Destroy
    public void destroy() {
        if (service != null) {
            service = null;
        }
        log.debug("Destroyed the seam component...");
    }

}

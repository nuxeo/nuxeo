/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.contentview.seam;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.contentview.jsf.ContentViewService;
import org.nuxeo.runtime.api.Framework;

/**
 * Business delegate exposting the {@link ContentViewService} as a seam
 * component.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
@Name("contentViewService")
@Scope(SESSION)
public class ContentViewBusinessDelegate implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ContentViewBusinessDelegate.class);

    protected ContentViewService contentViewService;

    /**
     * Acquires a new {@link ContentViewService} reference. The related service
     * may be deployed on a local or remote AppServer.
     *
     * @throws ClientException
     */
    @Unwrap
    public ContentViewService getService() throws ClientException {
        if (contentViewService == null) {
            try {
                contentViewService = Framework.getService(ContentViewService.class);
            } catch (Exception e) {
                final String errMsg = "Error connecting to ContentViewService. "
                        + e.getMessage();
                throw new ClientException(errMsg, e);
            }
            if (contentViewService == null) {
                throw new ClientException(
                        "ContentViewService service not bound");
            }
        }
        return contentViewService;
    }

    @Destroy
    public void destroy() {
        if (contentViewService != null) {
            contentViewService = null;
        }
        log.debug("Destroyed the seam component");
    }

}

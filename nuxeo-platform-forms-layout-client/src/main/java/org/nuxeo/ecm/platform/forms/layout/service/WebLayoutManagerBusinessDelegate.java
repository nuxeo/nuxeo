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
package org.nuxeo.ecm.platform.forms.layout.service;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.runtime.api.Framework;

/**
 * Exposes the {@link WebLayoutManager} service to Seam session context.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
@Name("webLayoutManager")
@Scope(ScopeType.SESSION)
public class WebLayoutManagerBusinessDelegate implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(WebLayoutManagerBusinessDelegate.class);

    protected WebLayoutManager webLayoutManager;

    /**
     * Acquires a new {@link WebLayoutManager} reference.
     */
    @Unwrap
    public WebLayoutManager getWebLayoutManager() throws ClientException {
        if (webLayoutManager == null) {
            try {
                webLayoutManager = Framework.getService(WebLayoutManager.class);
            } catch (Exception e) {
                final String errMsg = "Error connecting to WebLayoutManager. "
                        + e.getMessage();
                throw new ClientException(errMsg, e);
            }
            if (webLayoutManager == null) {
                throw new ClientException("WebLayoutManager service not bound");
            }

        }
        return webLayoutManager;
    }

    @Destroy
    public void destroy() {
        if (null != webLayoutManager) {
            webLayoutManager = null;
        }
        log.debug("Destroyed the seam component");
    }

}

/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.rendition.publisher;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.1
 */
@Name("renditionService")
@Scope(ScopeType.SESSION)
public class RenditionBusinessDelegate implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(RenditionBusinessDelegate.class);

    protected RenditionService renditionService;

    /**
     * Acquires a new {@link RenditionService} reference. The related service
     * may be deployed on a local or remote AppServer.
     *
     * @throws org.nuxeo.ecm.core.api.ClientException
     */
    @Unwrap
    public RenditionService getService() throws ClientException {
        if (renditionService == null) {
            try {
                renditionService = Framework.getService(RenditionService.class);
            } catch (Exception e) {
                final String errMsg = "Error connecting to RenditionService. "
                        + e.getMessage();
                throw new ClientException(errMsg, e);
            }
            if (renditionService == null) {
                throw new ClientException(
                        "ContentViewService service not bound");
            }
        }
        return renditionService;
    }

    @Destroy
    public void destroy() {
        if (renditionService != null) {
            renditionService = null;
        }
        log.debug("Destroyed the seam component");
    }

}

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

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;

import javax.annotation.security.PermitAll;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.api.ECM;
import org.nuxeo.ecm.platform.interfaces.ejb.ECServer;
import org.nuxeo.ecm.platform.ui.web.shield.NuxeoJavaBeanErrorHandler;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@Name("ecServer")
@Scope(CONVERSATION)
@NuxeoJavaBeanErrorHandler
public class ECServerBusinessDelegate implements Serializable {

    private static final long serialVersionUID = -8232067825574622128L;

    private static final Log log = LogFactory.getLog(ECServerBusinessDelegate.class);

    protected ECServer ecServer;

    //@Create
    public void initialize() {
        log.info("Seam component initialized...");
    }

    /**
     * Acquires a new {@link ECServer} reference. The related EJB may be
     * deployed on a local or remote AppServer.
     *
     * @return
     * @throws ClientException
     */
    @Unwrap
    public ECServer getECServer() throws ClientException {
        if (null == ecServer) {
            try {
                //ecServer = ECM.getPlatform().getService(ECServer.class);
                ecServer = Framework.getService(ECServer.class);
            } catch (Exception e) {
                final String errMsg = "Error connecting to ECServer. "
                        + e.getMessage();
                //log.error(errMsg, e);
                throw new ClientException(errMsg, e);
            }

            if (null == ecServer) {
                throw new ClientException("ECServer service not bound");
            }
        }

        return ecServer;
    }

    @Destroy
    @PermitAll
    public void destroy() throws ClientException {
        if (null != ecServer) {
            ecServer.remove();
            ecServer = null;
        }

        log.info("Destroyed the Seam component...");
    }

}

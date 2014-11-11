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
import org.nuxeo.ecm.platform.interfaces.ejb.ECDomain;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
// TODO: remove (not used)
@Deprecated
@Name("ecDomain")
@Scope(CONVERSATION)
public class ECDomainBusinessDelegate implements Serializable {

    private static final long serialVersionUID = -9197504420097544988L;

    private static final Log log = LogFactory.getLog(ECDomainBusinessDelegate.class);

    protected ECDomain ecDomain;

    //@Create
    public void initialize() {
        log.info("Seam component initialized...");
    }

    /**
     * Acquires a new {@link ECDomain} reference. The related EJB may be
     * deployed on a local or remote AppServer.
     */
    @Unwrap
    public ECDomain getECDomain() throws ClientException {
        if (null == ecDomain) {
            try {
                //ecDomain = ECM.getPlatform().getService(ECDomain.class);
                ecDomain = Framework.getService(ECDomain.class);
            } catch (Exception e) {
                final String errMsg = "Error connecting to ECDomain. "
                        + e.getMessage();
                //log.error(errMsg, e);
                throw new ClientException(errMsg, e);
            }

            if (null == ecDomain) {
                throw new ClientException("ECDomain service not bound");
            }
        }
        return ecDomain;
    }

    @Destroy
    @PermitAll
    public void destroy() {
        if (null != ecDomain) {
            ecDomain.remove();
            ecDomain = null;
        }
        log.info("Destroyed the seam component...");
    }

}

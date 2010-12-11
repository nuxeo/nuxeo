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
 *     Wojciech Sulejman
 *
 */
package org.nuxeo.ecm.platform.signature.web.sign;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.signature.api.user.CertUserService;
import org.nuxeo.runtime.api.Framework;

/**
 * UserInfo service provider
 *
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 */

@Name("certUserService")
@Scope(SESSION)
public class CertUserServiceBusinessDelegate implements Serializable {

    private static final long serialVersionUID = 3L;

    private static final Log log = LogFactory.getLog(CertUserServiceBusinessDelegate.class);

    protected CertUserService certUserService;

    @Unwrap
    public CertUserService getService() throws ClientException {
        if (certUserService == null) {
            try {
                certUserService = Framework.getService(CertUserService.class);
            } catch (Exception e) {
                final String errMsg = "Error connecting to CertUserService. "
                        + e.getMessage();
                throw new ClientException(errMsg, e);
            }
            if (certUserService == null) {
                throw new ClientException("CertUserService service not bound");
            }
        }
        return certUserService;
    }

    @Destroy
    public void destroy() {
        if (certUserService != null) {
            certUserService = null;
        }
        log.debug("Destroyed the seam component");
    }
}
/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import org.nuxeo.ecm.platform.signature.api.sign.SignatureService;
import org.nuxeo.runtime.api.Framework;

/**
 * Signature service provider
 *
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 */

@Name("signatureService")
@Scope(SESSION)
public class SignatureServiceBusinessDelegate implements Serializable {

    private static final long serialVersionUID = 2L;

    private static final Log log = LogFactory.getLog(SignatureServiceBusinessDelegate.class);

    protected transient SignatureService signatureService;

    @Unwrap
    public SignatureService getService() {
        if (signatureService == null) {
            signatureService = Framework.getService(SignatureService.class);
        }
        return signatureService;
    }

    @Destroy
    public void destroy() {
        if (signatureService != null) {
            signatureService = null;
        }
        log.debug("Destroyed the seam component");
    }

}

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
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@Name("typeManager")
@Scope(CONVERSATION)
public class TypeManagerBusinessDelegate implements Serializable {

    private static final long serialVersionUID = -5326113474071108997L;

    private static final Log log = LogFactory.getLog(TypeManagerBusinessDelegate.class);

    protected TypeManager typeManager;

    // @Create
    public void initialize() {
        log.info("Seam component initialized...");
    }

    /**
     * Acquires a new {@link TypeManager} reference. The related EJB may be
     * deployed on a local or remote AppServer.
     *
     * @return
     * @throws ClientException
     */
    @Unwrap
    public TypeManager getTypeManager() throws ClientException {
        if (null == typeManager) {
            try {
                typeManager = Framework.getService(TypeManager.class);
            } catch (Exception e) {
                final String errMsg = "Error connecting to TypeManager. "
                        + e.getMessage();
                // log.error(errMsg, e);
                throw new ClientException(errMsg, e);
            }

            if (null == typeManager) {
                throw new ClientException("TypeManager service not bound");
            }
        }

        return typeManager;
    }

    @Destroy
    @PermitAll
    public void destroy() {
        if (null != typeManager) {
            //typeManager.remove();
            typeManager = null;
        }
    }
}
